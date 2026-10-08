package com.andresexpress.orders.domain.model;

import java.math.BigDecimal;

/** Respuesta de Coverage: nombres oficiales de API Colombia (con tildes) y la tarifa. */
public record TariffResult(
        String originCity,
        String originDepartment,
        String destinationCity,
        String destinationDepartment,
        BigDecimal totalTariff) {
}
