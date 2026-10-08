package com.andesexpress.coverage.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ValidateCoverageDTO {
    @NotBlank
    private String originCity;

    @NotBlank
    private String originDepartment;

    @NotBlank
    private String destinationCity;

    @NotBlank
    private String destinationDepartment;

    @NotNull
    @Positive
    private Double weight;
}
