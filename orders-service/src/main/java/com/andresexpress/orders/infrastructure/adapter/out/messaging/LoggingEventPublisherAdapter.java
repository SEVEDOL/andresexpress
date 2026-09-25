package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.application.port.out.EventPublisherPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LoggingEventPublisherAdapter implements EventPublisherPort {

    @Override
    public void publishOrderCreated(OrderDomain order) {
        log.info("[EVENTO SIMULADO] OrderCreated -> pedido {}", order.getOrderId());
    }
}
