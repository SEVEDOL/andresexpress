package com.andesexpress.waybill;

import java.math.BigDecimal;

/** Pedidos de ejemplo para las pruebas. */
final class Orders {

    static final String TRACKING = "ANDES-12345678";
    /** SHA-256 de TRACKING calculado fuera de Java (sha256sum): debe coincidir con OrderDomain.calculateHash de Orders. */
    static final String TRACKING_HASH = "f2cff04b06d6ee2d0dbc31a807083d653f2de9daed45ebb6d1c20deb41c98406";

    private Orders() {
    }

    static OrderCreatedEvent anOrder() {
        return new OrderCreatedEvent("804b347f-d487-468e-b0f2-f8d80c96a6b3", TRACKING,
                "Medellín", "Antioquia", "Rionegro", "Santander", 3.5, "EXPRESS", new BigDecimal("28000"),
                "Ana Torres", "3001234567", "Luis Peña", "3007654321");
    }

    static String json(String trackingNumber) {
        String tracking = trackingNumber == null ? "null" : "\"" + trackingNumber + "\"";
        return "{\"eventId\":\"804b347f-d487-468e-b0f2-f8d80c96a6b3\",\"senderEmail\":\"ana@example.com\","
                + "\"plainTrackingNumber\":" + tracking + ",\"originCity\":\"Medellín\",\"originDepartment\":\"Antioquia\","
                + "\"destinationCity\":\"Rionegro\",\"destinationDepartment\":\"Santander\",\"weight\":3.5,"
                + "\"shipmentType\":\"EXPRESS\",\"totalTariff\":28000,\"senderName\":\"Ana Torres\","
                + "\"senderPhone\":\"3001234567\",\"recipientName\":\"Luis Peña\",\"recipientPhone\":\"3007654321\"}";
    }
}
