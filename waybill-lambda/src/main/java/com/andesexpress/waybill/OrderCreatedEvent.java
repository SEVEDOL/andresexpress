package com.andesexpress.waybill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/** Evento "pedido creado" que publica Orders: mismos nombres de campo que su OrderCreatedEvent. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedEvent(
        String eventId,
        String plainTrackingNumber,
        String originCity,
        String originDepartment,
        String destinationCity,
        String destinationDepartment,
        Double weight,
        String shipmentType,
        BigDecimal totalTariff,
        String senderName,
        String senderPhone,
        String recipientName,
        String recipientPhone) {
}
