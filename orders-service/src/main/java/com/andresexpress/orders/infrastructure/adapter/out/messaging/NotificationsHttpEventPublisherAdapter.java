package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.application.port.out.EventPublisherPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Alternativa SIN cola: llama por HTTP a Notifications y espera a que envie el correo.
 * Solo se usa con events.publisher=http (por ejemplo, en local sin Docker).
 * Por defecto se publica en SQS (SqsEventPublisherAdapter).
 *
 * Si Notifications no responde, lanza la excepcion; CreateOrderService la captura y
 * el pedido queda creado igual (RN-10).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "events.publisher", havingValue = "http")
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
        notificationsClient.post()
                .uri("/api/v1/notifications/simulate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(OrderCreatedEvent.from(order))
                .retrieve()
                .toBodilessEntity();

        log.info("Evento OrderCreated enviado a Notifications para el pedido {}", order.getOrderId());
    }
}
