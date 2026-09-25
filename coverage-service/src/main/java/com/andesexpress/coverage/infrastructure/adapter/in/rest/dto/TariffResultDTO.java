package com.andesexpress.coverage.infrastructure.adapter.in.rest.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class TariffResultDTO {
    private String originCity;
    private String destinationCity;
    private String originDepartment;
    private String destinationDepartment;
    private Double weight;
    private BigDecimal totalTariff;
}