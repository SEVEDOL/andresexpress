package com.andesexpress.coverage.domain.model;

import java.math.BigDecimal;

/** Resultado de validar cobertura y calcular la tarifa. */
public record TariffResult(
        String originCity,
        String destinationCity,
        String originDepartment,
        String destinationDepartment,
        Double weight,
        Zone zone,
        BigDecimal totalTariff) {
}
