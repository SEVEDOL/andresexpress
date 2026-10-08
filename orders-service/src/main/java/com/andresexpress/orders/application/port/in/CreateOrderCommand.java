package com.andresexpress.orders.application.port.in;

import com.andresexpress.orders.domain.model.ShipmentType;

public record CreateOrderCommand(
        String originCity,
        String originDepartment,
        String destinationCity,
        String destinationDepartment,
        Double weight,
        ShipmentType shipmentType,
        String senderName,
        String senderEmail,
        String senderPhone,
        String recipientName,
        String recipientPhone) {
}
