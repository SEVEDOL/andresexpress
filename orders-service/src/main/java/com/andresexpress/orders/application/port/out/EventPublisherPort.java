package com.andresexpress.orders.application.port.out;

import com.andresexpress.orders.domain.model.OrderDomain;

public interface EventPublisherPort {
    void publishOrderCreated(OrderDomain order);
}
