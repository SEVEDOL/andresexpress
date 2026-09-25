package com.andesexpress.notifications.application.port.in;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Evento "pedido creado" que llega desde Orders.
 * Trae la misma informacion de la guia para armar el correo (RF-10).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEventCommand {
    private String eventId;
    private String senderEmail;
    private String senderName;
    private String senderPhone;
    private String plainTrackingNumber;
    private String recipientName;
    private String recipientPhone;
    private String originCity;
    private String destinationCity;
    private Double weight;
    private String shipmentType;
    private BigDecimal totalTariff;
}
