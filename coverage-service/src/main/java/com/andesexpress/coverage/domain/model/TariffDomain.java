package com.andesexpress.coverage.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * Reglas de negocio de la tarifa (sin dependencias externas).
 * - La ZONA se decide aqui (misma ciudad, mismo departamento, resto del pais).
 * - Los VALORES de cada zona vienen de afuera (ZoneTariff, guardado en DynamoDB).
 */
@Getter
public class TariffDomain {

    private final Double weight;
    private final String originDepartment;
    private final String destinationDepartment;
    private final String originCity;
    private final String destinationCity;

    public TariffDomain(String originCity, String destinationCity, String originDepartment, String destinationDepartment, Double weight) {
        this.originCity = originCity;
        this.destinationCity = destinationCity;
        this.originDepartment = originDepartment;
        this.destinationDepartment = destinationDepartment;
        this.weight = weight;
    }

    public Zone determineZone() {
        if (originCity.equalsIgnoreCase(destinationCity)) {
            return Zone.SAME_CITY;
        }
        if (originDepartment.equalsIgnoreCase(destinationDepartment)) {
            return Zone.SAME_DEPARTMENT;
        }
        return Zone.REST_OF_COUNTRY;
    }

    /** Tarifa = base de la zona + recargo por cada kg que supere los kilos incluidos. */
    public BigDecimal calculateTotalCost(ZoneTariff rate) {
        BigDecimal weightKg = BigDecimal.valueOf(weight);
        BigDecimal excessKg = weightKg.subtract(rate.includedKg()).max(BigDecimal.ZERO);
        return rate.baseTariff().add(excessKg.multiply(rate.extraKgRate()));
    }
}
