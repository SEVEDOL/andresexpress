package com.andresexpress.orders.infrastructure.adapter.in.rest.dto;

import com.andresexpress.orders.domain.model.ShipmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank String originCity,
        @NotBlank String destinationCity,
        @NotNull @Positive Double weight,
        @NotNull ShipmentType shipmentType,
        @NotBlank String senderName,
        @NotBlank @Email String senderEmail,
        @NotBlank String senderPhone,
        @NotBlank String recipientName,
        @NotBlank String recipientPhone) {
}
