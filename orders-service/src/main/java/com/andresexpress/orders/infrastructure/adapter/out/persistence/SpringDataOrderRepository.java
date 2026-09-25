package com.andresexpress.orders.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataOrderRepository extends JpaRepository<OrderEntity, UUID> {
    Optional<OrderEntity> findByHashedTrackingNumber(String hashedTrackingNumber);
    boolean existsByHashedTrackingNumber(String hashedTrackingNumber);
}
