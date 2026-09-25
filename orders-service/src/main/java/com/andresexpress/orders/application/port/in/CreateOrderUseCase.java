package com.andresexpress.orders.application.port.in;

import com.andresexpress.orders.domain.model.OrderDomain;

public interface CreateOrderUseCase {
    OrderDomain createOrder(CreateOrderCommand command);
}
