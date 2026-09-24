package com.andresexpress.orders.infrastructure.adapter.out.persistence;

import com.andresexpress.orders.application.port.out.OrderRepositoryPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final SpringDataOrderRepository repository;

    @Override
    public OrderDomain save(OrderDomain order) {
        return toDomain(repository.save(toEntity(order)));
    }

    @Override
    public Optional<OrderDomain> findByHashedTracking(String hashedTrackingNumber) {
        return repository.findByHashedTrackingNumber(hashedTrackingNumber).map(this::toDomain);
    }

    @Override
    public boolean existsByHashedTracking(String hashedTrackingNumber) {
        return repository.existsByHashedTrackingNumber(hashedTrackingNumber);
    }

    private OrderEntity toEntity(OrderDomain d) {
        OrderEntity e = new OrderEntity();
        e.setId(d.getOrderId());
        e.setHashedTrackingNumber(d.getHashedTrackingNumber());
        e.setOriginCity(d.getOriginCity());
        e.setDestinationCity(d.getDestinationCity());
        e.setWeight(d.getWeight());
        e.setShipmentType(d.getShipmentType());
        e.setTotalTariff(d.getTotalTariff());
        e.setSenderName(d.getSenderName());
        e.setSenderEmail(d.getSenderEmail());
        e.setSenderPhone(d.getSenderPhone());
        e.setRecipientName(d.getRecipientName());
        e.setRecipientPhone(d.getRecipientPhone());
        e.setStatus(d.getStatus());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }

    private OrderDomain toDomain(OrderEntity e) {
        return OrderDomain.builder()
                .orderId(e.getId())
                .hashedTrackingNumber(e.getHashedTrackingNumber())
                .originCity(e.getOriginCity())
                .destinationCity(e.getDestinationCity())
                .weight(e.getWeight())
                .shipmentType(e.getShipmentType())
                .totalTariff(e.getTotalTariff())
                .senderName(e.getSenderName())
                .senderEmail(e.getSenderEmail())
                .senderPhone(e.getSenderPhone())
                .recipientName(e.getRecipientName())
                .recipientPhone(e.getRecipientPhone())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
