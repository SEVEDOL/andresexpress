package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.application.port.out.EventPublisherPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Publica el evento "pedido creado" llamando por HTTP al microservicio Notifications.
 * Es una version LOCAL: en AWS se reemplazara por un adaptador que publique en SNS/SQS,
 * sin tocar el caso de uso (solo cambia este adaptador).
 *
 * Si Notifications no responde, lanza la excepcion; CreateOrderService la captura y
 * el pedido queda creado igual (RN-10).
 */
@Slf4j
@Component
public class NotificationsHttpEventPublisherAdapter implements EventPublisherPort {

    private final RestClient notificationsClient;

    public NotificationsHttpEventPublisherAdapter(@Value("${notifications.url}") String notificationsUrl,
                                                  @Value("${notifications.timeout-ms}") long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        this.notificationsClient = RestClient.builder()
                .baseUrl(notificationsUrl)
                .requestFactory(factory)
                .build();
    }

    @Override
    public void publishOrderCreated(OrderDomain order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
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
                order.getDestinationCity(),
                order.getWeight(),
                order.getShipmentType() != null ? order.getShipmentType().name() : null,
                order.getTotalTariff()
        );

        notificationsClient.post()
                .uri("/api/v1/notifications/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(event)
                .retrieve()
                .toBodilessEntity();

        log.info("Evento OrderCreated enviado a Notifications para el pedido {}", order.getOrderId());
    }

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
            String destinationCity,
            Double weight,
            String shipmentType,
            BigDecimal totalTariff) {
    }
}
