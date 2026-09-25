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
    private String destinationCity;

    @NotNull
    @Positive
    private Double weight;
}