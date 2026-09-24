package com.andresexpress.orders.application.port.out;

import com.andresexpress.orders.domain.model.OrderDomain;

import java.util.Optional;

public interface OrderRepositoryPort {
    OrderDomain save(OrderDomain order);
    Optional<OrderDomain> findByHashedTracking(String hashedTrackingNumber);
    boolean existsByHashedTracking(String hashedTrackingNumber);
}
