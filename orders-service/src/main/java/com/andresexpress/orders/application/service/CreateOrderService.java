package com.andresexpress.orders.application.service;

import com.andresexpress.orders.application.port.in.CreateOrderCommand;
import com.andresexpress.orders.application.port.in.CreateOrderUseCase;
import com.andresexpress.orders.application.port.out.CoverageServicePort;
import com.andresexpress.orders.application.port.out.EventPublisherPort;
import com.andresexpress.orders.application.port.out.OrderRepositoryPort;
import com.andresexpress.orders.application.port.out.WaybillRequestPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import com.andresexpress.orders.domain.model.TariffResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateOrderService implements CreateOrderUseCase {

    private final CoverageServicePort coverageServicePort;
    private final OrderRepositoryPort orderRepositoryPort;
    private final EventPublisherPort eventPublisherPort;
    private final WaybillRequestPort waybillRequestPort;

    @Override
    public OrderDomain createOrder(CreateOrderCommand command) {
        // 1. Validar ciudades y calcular tarifa (si falla, lanza excepción y no se guarda nada)
        TariffResult tariff = coverageServicePort.validateAndCalculateTariff(
                command.originCity(), command.originDepartment(),
                command.destinationCity(), command.destinationDepartment(), command.weight());

        // 2. Armar el pedido y confirmarlo (id, guía, hash, estado).
        //    Se guardan los nombres oficiales que devolvió Coverage, no los que escribió el cliente.
        OrderDomain order = OrderDomain.builder()
                .originCity(tariff.originCity())
                .originDepartment(tariff.originDepartment())
                .destinationCity(tariff.destinationCity())
                .destinationDepartment(tariff.destinationDepartment())
                .weight(command.weight())
                .shipmentType(command.shipmentType())
                .senderName(command.senderName())
                .senderEmail(command.senderEmail())
                .senderPhone(command.senderPhone())
                .recipientName(command.recipientName())
                .recipientPhone(command.recipientPhone())
                .build()
                .confirm(generateUniqueTrackingNumber(), tariff.totalTariff());

        // 3. Guardar
        orderRepositoryPort.save(order);

        // 4. Publicar evento. Si falla, el pedido YA existe (RN-10): solo se registra el error
        try {
            eventPublisherPort.publishOrderCreated(order);
        } catch (Exception e) {
            log.error("No se pudo publicar el evento del pedido {}", order.getOrderId(), e);
        }

        // 5. Pedir la guía en PDF. Si falla, el pedido también queda creado (RN-10)
        //    y el cliente puede regenerarla después con su número de guía (RF-09)
        try {
            waybillRequestPort.requestWaybill(order);
        } catch (Exception e) {
            log.error("No se pudo solicitar la guía en PDF del pedido {}", order.getOrderId(), e);
        }

        // Se devuelve 'order' (no lo que retorna save) porque es el único que tiene el número plano
        return order;
    }

    private String generateUniqueTrackingNumber() {
        String candidate;
        do {
            candidate = OrderDomain.generateTrackingNumber();
        } while (orderRepositoryPort.existsByHashedTracking(OrderDomain.calculateHash(candidate)));
        return candidate;
    }
}
