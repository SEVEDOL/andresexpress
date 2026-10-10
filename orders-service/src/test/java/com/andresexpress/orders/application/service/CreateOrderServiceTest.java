package com.andresexpress.orders.application.service;

import com.andresexpress.orders.application.port.in.CreateOrderCommand;
import com.andresexpress.orders.application.port.out.CoverageServicePort;
import com.andresexpress.orders.application.port.out.EventPublisherPort;
import com.andresexpress.orders.application.port.out.OrderRepositoryPort;
import com.andresexpress.orders.application.port.out.WaybillRequestPort;
import com.andresexpress.orders.domain.exception.InvalidCityException;
import com.andresexpress.orders.domain.model.OrderDomain;
import com.andresexpress.orders.domain.model.OrderStatus;
import com.andresexpress.orders.domain.model.ShipmentType;
import com.andresexpress.orders.domain.model.TariffResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private CoverageServicePort coverageServicePort;
    @Mock
    private OrderRepositoryPort orderRepositoryPort;
    @Mock
    private EventPublisherPort eventPublisherPort;
    @Mock
    private WaybillRequestPort waybillRequestPort;

    @InjectMocks
    private CreateOrderService createOrderService;

    private static CreateOrderCommand aCommand() {
        return new CreateOrderCommand("medellin", "antioquia", "rionegro", "santander", 3.5, ShipmentType.EXPRESS,
                "Ana Torres", "ana@example.com", "3001234567", "Luis Peña", "3007654321");
    }

    private void coverageAccepts() {
        when(coverageServicePort.validateAndCalculateTariff("medellin", "antioquia", "rionegro", "santander", 3.5))
                .thenReturn(new TariffResult("Medellín", "Antioquia", "Rionegro", "Santander", new BigDecimal("28000")));
    }

    @Test
    @DisplayName("RF-01/RF-07: guarda el pedido, publica el evento y pide la guia en PDF")
    void savesPublishesAndRequestsTheWaybill() {
        coverageAccepts();

        OrderDomain order = createOrderService.createOrder(aCommand());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getTotalTariff()).isEqualByComparingTo("28000");
        // Se guardan los nombres oficiales que devolvio Coverage, no lo que escribio el cliente
        assertThat(order.getDestinationCity()).isEqualTo("Rionegro");
        assertThat(order.getDestinationDepartment()).isEqualTo("Santander");
        verify(orderRepositoryPort).save(order);
        verify(eventPublisherPort).publishOrderCreated(order);
        verify(waybillRequestPort).requestWaybill(order);
    }

    @Test
    @DisplayName("la guia se pide con el numero en claro, que solo existe en memoria al crear el pedido")
    void waybillIsRequestedWithThePlainTrackingNumber() {
        coverageAccepts();

        createOrderService.createOrder(aCommand());

        ArgumentCaptor<OrderDomain> requested = ArgumentCaptor.forClass(OrderDomain.class);
        verify(waybillRequestPort).requestWaybill(requested.capture());
        assertThat(requested.getValue().getPlainTrackingNumber()).matches("ANDES-\\d{8}");
        assertThat(requested.getValue().getHashedTrackingNumber())
                .isEqualTo(OrderDomain.calculateHash(requested.getValue().getPlainTrackingNumber()));
    }

    @Test
    @DisplayName("RF-02/03: si Coverage rechaza la ciudad, no se guarda, ni se publica, ni se pide guia")
    void invalidCityCreatesNothing() {
        when(coverageServicePort.validateAndCalculateTariff(anyString(), anyString(), anyString(), anyString(), any()))
                .thenThrow(new InvalidCityException("La ciudad de origen o destino no existe en el departamento indicado"));

        assertThatThrownBy(() -> createOrderService.createOrder(aCommand())).isInstanceOf(InvalidCityException.class);

        verify(orderRepositoryPort, never()).save(any());
        verify(eventPublisherPort, never()).publishOrderCreated(any());
        verify(waybillRequestPort, never()).requestWaybill(any());
    }

    @Test
    @DisplayName("RN-10: si falla la solicitud de la guia en PDF, el pedido queda creado igual")
    void waybillRequestFailureDoesNotBreakTheOrder() {
        coverageAccepts();
        doThrow(new IllegalStateException("cola caida")).when(waybillRequestPort).requestWaybill(any());

        OrderDomain order = createOrderService.createOrder(aCommand());

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepositoryPort).save(order);
    }

    @Test
    @DisplayName("RN-10: si falla el evento de notificacion, el pedido se crea y la guia se pide de todas formas")
    void eventFailureStillRequestsTheWaybill() {
        coverageAccepts();
        doThrow(new IllegalStateException("cola caida")).when(eventPublisherPort).publishOrderCreated(any());

        OrderDomain order = createOrderService.createOrder(aCommand());

        verify(orderRepositoryPort).save(order);
        verify(waybillRequestPort).requestWaybill(order);
    }

    @Test
    @DisplayName("si el numero de guia generado ya existe, se genera otro")
    void retriesWhenTheTrackingNumberAlreadyExists() {
        coverageAccepts();
        when(orderRepositoryPort.existsByHashedTracking(anyString())).thenReturn(true, false);

        createOrderService.createOrder(aCommand());

        verify(orderRepositoryPort, times(2)).existsByHashedTracking(anyString());
    }
}
