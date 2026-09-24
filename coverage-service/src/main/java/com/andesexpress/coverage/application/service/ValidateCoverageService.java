package com.andesexpress.coverage.application.service;

import com.andesexpress.coverage.application.port.in.ValidateCoverageUseCase;
import com.andesexpress.coverage.application.port.out.ApiColombiaPort;
import com.andesexpress.coverage.domain.exception.CityNotFoundException;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.domain.model.TariffDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidateCoverageService implements ValidateCoverageUseCase {

    private final ApiColombiaPort apiColombiaPort;

    @Override
    public TariffDomain validateAndCalculate(String originCity, String destinationCity, Double weight) {
        CityData origin = apiColombiaPort.fetchCityByName(originCity)
                .orElseThrow(() -> new CityNotFoundException(originCity));

        CityData destination = apiColombiaPort.fetchCityByName(destinationCity)
                .orElseThrow(() -> new CityNotFoundException(destinationCity));

        return new TariffDomain(
                origin.getName(),
                destination.getName(),
                origin.getDepartmentName(),
                destination.getDepartmentName(),
                weight
        );
    }
}