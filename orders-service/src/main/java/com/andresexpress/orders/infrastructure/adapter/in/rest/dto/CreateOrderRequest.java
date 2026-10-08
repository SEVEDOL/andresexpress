package com.andresexpress.orders.infrastructure.adapter.in.rest.dto;

import com.andresexpress.orders.domain.model.ShipmentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderRequest(
        @NotBlank(message = "es obligatoria") String originCity,
        @NotBlank(message = "es obligatoria") String destinationCity,
        @NotNull(message = "es obligatorio") @Positive(message = "debe ser mayor que 0") Double weight,
        @NotNull(message = "es obligatorio (STANDARD o EXPRESS)") ShipmentType shipmentType,
        @NotBlank(message = "es obligatorio") String senderName,
        @NotBlank(message = "es obligatorio") @Email(message = "no tiene un formato de correo válido") String senderEmail,
        @NotBlank(message = "es obligatorio") String senderPhone,
        @NotBlank(message = "es obligatorio") String recipientName,
        @NotBlank(message = "es obligatorio") String recipientPhone) {
}
