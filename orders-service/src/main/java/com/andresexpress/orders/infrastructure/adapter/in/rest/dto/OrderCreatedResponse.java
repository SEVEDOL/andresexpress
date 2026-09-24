package com.andresexpress.orders.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

public record OrderCreatedResponse(String trackingNumber, BigDecimal totalTariff, String status) {
}
