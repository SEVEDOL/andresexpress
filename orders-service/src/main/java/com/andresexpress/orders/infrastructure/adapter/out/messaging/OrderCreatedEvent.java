package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.domain.model.OrderDomain;

import java.math.BigDecimal;

/** Contrato del evento: mismos nombres de campo que OrderCreatedEventCommand en Notifications. */
public record OrderCreatedEvent(
        String eventId,
        String senderEmail,
        String senderName,
        String senderPhone,
        String plainTrackingNumber,
        String recipientName,
        String recipientPhone,
        String originCity,
        String originDepartment,
        String destinationCity,
        String destinationDepartment,
        Double weight,
        String shipmentType,
        BigDecimal totalTariff) {

    public static OrderCreatedEvent from(OrderDomain order) {
        return new OrderCreatedEvent(
                // Un pedido = un evento: usar el orderId como eventId permite que
                // Notifications descarte duplicados (idempotencia).
                order.getOrderId().toString(),
                order.getSenderEmail(),
                order.getSenderName(),
                order.getSenderPhone(),
                order.getPlainTrackingNumber(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getOriginCity(),
                order.getOriginDepartment(),
                order.getDestinationCity(),
                order.getDestinationDepartment(),
                order.getWeight(),
                order.getShipmentType() != null ? order.getShipmentType().name() : null,
                order.getTotalTariff());
    }
}
