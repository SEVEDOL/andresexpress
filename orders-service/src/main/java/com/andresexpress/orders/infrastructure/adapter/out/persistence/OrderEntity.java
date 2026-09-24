package com.andresexpress.orders.infrastructure.adapter.out.persistence;

import com.andresexpress.orders.domain.model.OrderStatus;
import com.andresexpress.orders.domain.model.ShipmentType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "hashed_tracking_number", nullable = false, unique = true, length = 64)
    private String hashedTrackingNumber;

    @Column(nullable = false)
    private String originCity;

    @Column(nullable = false)
    private String destinationCity;

    @Column(nullable = false)
    private Double weight;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShipmentType shipmentType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalTariff;

    private String senderName;
    private String senderEmail;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;
}
