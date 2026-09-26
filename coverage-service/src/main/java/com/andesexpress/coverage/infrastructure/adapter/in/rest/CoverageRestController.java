package com.andesexpress.coverage.infrastructure.adapter.in.rest;

import com.andesexpress.coverage.application.port.in.ValidateCoverageUseCase;
import com.andesexpress.coverage.domain.model.TariffResult;
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
        TariffResult result = validateCoverageUseCase.validateAndCalculate(
                dto.getOriginCity(),
                dto.getDestinationCity(),
                dto.getWeight()
        );

        TariffResultDTO response = TariffResultDTO.builder()
                .originCity(result.originCity())
                .destinationCity(result.destinationCity())
                .originDepartment(result.originDepartment())
                .destinationDepartment(result.destinationDepartment())
                .weight(result.weight())
                .totalTariff(result.totalTariff())
                .build();

        return ResponseEntity.ok(response);
    }
}