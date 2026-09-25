package com.andesexpress.coverage.domain.model;

import java.math.BigDecimal;
import lombok.Getter;

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

    public BigDecimal calculateTotalCost() {
        BigDecimal baseTariff;

        // Regla 1: Tarifa Base según ubicación
        if (originCity.equalsIgnoreCase(destinationCity)) {
            baseTariff = new BigDecimal("10000");
        } else if (originDepartment.equalsIgnoreCase(destinationDepartment)) {
            baseTariff = new BigDecimal("15000");
        } else {
            baseTariff = new BigDecimal("25000");
        }

        // Regla 2: Recargo por peso si supera los 2.0 kg ($2.000 COP por kg excedente)
        BigDecimal excessCharge = BigDecimal.ZERO;
        if (weight > 2.0) {
            double excessWeight = weight - 2.0;
            excessCharge = BigDecimal.valueOf(excessWeight * 2000);
        }

        return baseTariff.add(excessCharge);
    }
}