package com.andresexpress.orders.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
public class OrderDomain {

    private static final SecureRandom RANDOM = new SecureRandom();

    private UUID orderId;
    private String plainTrackingNumber;   // solo en memoria: NUNCA se guarda en BD
    private String hashedTrackingNumber;  // esto es lo que se guarda
    private String originCity;
    private String destinationCity;
    private Double weight;
    private ShipmentType shipmentType;
    private BigDecimal totalTariff;
    private String senderName;
    private String senderEmail;
    private String senderPhone;
    private String recipientName;
    private String recipientPhone;
    private OrderStatus status;
    private LocalDateTime createdAt;

    public OrderDomain confirm(String trackingNumber, BigDecimal tariff) {
        return this.toBuilder()
                .orderId(UUID.randomUUID())
                .plainTrackingNumber(trackingNumber)
                .hashedTrackingNumber(calculateHash(trackingNumber))
                .totalTariff(tariff)
                .status(OrderStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static String generateTrackingNumber() {
        int number = 10_000_000 + RANDOM.nextInt(90_000_000);
        return "ANDES-" + number;
    }

    public static String calculateHash(String plainTrackingNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainTrackingNumber.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
