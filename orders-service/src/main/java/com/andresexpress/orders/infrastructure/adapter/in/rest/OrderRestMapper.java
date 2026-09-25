package com.andresexpress.orders.infrastructure.adapter.in.rest;

import com.andresexpress.orders.application.port.in.CreateOrderCommand;
import com.andresexpress.orders.domain.model.OrderDomain;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.CreateOrderRequest;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.OrderCreatedResponse;
import com.andresexpress.orders.infrastructure.adapter.in.rest.dto.WaybillResponse;

public final class OrderRestMapper {

    private OrderRestMapper() {
    }

    public static CreateOrderCommand toCommand(CreateOrderRequest r) {
        return new CreateOrderCommand(r.originCity(), r.destinationCity(), r.weight(), r.shipmentType(),
                r.senderName(), r.senderEmail(), r.senderPhone(), r.recipientName(), r.recipientPhone());
    }

    public static OrderCreatedResponse toCreatedResponse(OrderDomain o) {
        return new OrderCreatedResponse(o.getPlainTrackingNumber(), o.getTotalTariff(), o.getStatus().name());
    }

    public static WaybillResponse toWaybillResponse(OrderDomain o, String trackingNumber) {
        return new WaybillResponse(trackingNumber.trim().toUpperCase(), o.getOriginCity(), o.getDestinationCity(),
                o.getWeight(), o.getShipmentType().name(), o.getTotalTariff(), o.getSenderName(),
                o.getSenderPhone(), o.getRecipientName(), o.getRecipientPhone(), o.getStatus().name(),
                o.getCreatedAt());
    }
}
