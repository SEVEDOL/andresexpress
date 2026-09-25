package com.andresexpress.orders.infrastructure.adapter.out.external;

import java.math.BigDecimal;

public record CoverageResponse(
        String originCity,
        String destinationCity,
        String originDepartment,
        String destinationDepartment,
        Double weight,
        BigDecimal totalTariff) {
}
