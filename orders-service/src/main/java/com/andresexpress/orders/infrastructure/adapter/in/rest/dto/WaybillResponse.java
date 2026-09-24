package com.andresexpress.orders.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WaybillResponse(
        String trackingNumber,
        String originCity,
        String destinationCity,
        Double weight,
        String shipmentType,
        BigDecimal totalTariff,
        String senderName,
        String senderPhone,
        String recipientName,
        String recipientPhone,
        String status,
        LocalDateTime createdAt) {
}
