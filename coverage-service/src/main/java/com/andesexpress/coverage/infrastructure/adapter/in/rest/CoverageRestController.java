package com.andesexpress.coverage.infrastructure.adapter.in.rest;

import com.andesexpress.coverage.application.port.in.ValidateCoverageUseCase;
import com.andesexpress.coverage.domain.model.TariffDomain;
import com.andesexpress.coverage.infrastructure.adapter.in.rest.dto.TariffResultDTO;
import com.andesexpress.coverage.infrastructure.adapter.in.rest.dto.ValidateCoverageDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/coverage")
@RequiredArgsConstructor
public class CoverageRestController {

    private final ValidateCoverageUseCase validateCoverageUseCase;

    @PostMapping("/validate")
    public ResponseEntity<TariffResultDTO> validateCoverage(@Valid @RequestBody ValidateCoverageDTO dto) {
        TariffDomain domain = validateCoverageUseCase.validateAndCalculate(
                dto.getOriginCity(),
                dto.getDestinationCity(),
                dto.getWeight()
        );

        TariffResultDTO response = TariffResultDTO.builder()
                .originCity(domain.getOriginCity())
                .destinationCity(domain.getDestinationCity())
                .originDepartment(domain.getOriginDepartment())
                .destinationDepartment(domain.getDestinationDepartment())
                .weight(domain.getWeight())
                .totalTariff(domain.calculateTotalCost())
                .build();

        return ResponseEntity.ok(response);
    }
}