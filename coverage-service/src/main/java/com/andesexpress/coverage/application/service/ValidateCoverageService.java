package com.andesexpress.coverage.application.service;

import com.andesexpress.coverage.application.port.in.ValidateCoverageUseCase;
import com.andesexpress.coverage.application.port.out.ApiColombiaPort;
import com.andesexpress.coverage.application.port.out.TariffRatePort;
import com.andesexpress.coverage.domain.exception.CityNotFoundException;
import com.andesexpress.coverage.domain.exception.TariffNotConfiguredException;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.domain.model.TariffDomain;
import com.andesexpress.coverage.domain.model.TariffResult;
import com.andesexpress.coverage.domain.model.Zone;
import com.andesexpress.coverage.domain.model.ZoneTariff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ValidateCoverageService implements ValidateCoverageUseCase {

    private final ApiColombiaPort apiColombiaPort;
    private final TariffRatePort tariffRatePort;

    @Override
    public TariffResult validateAndCalculate(String originCity, String destinationCity, Double weight) {
        // 1. Validar ciudades contra la fuente oficial (API Colombia)
        CityData origin = apiColombiaPort.fetchCityByName(originCity)
                .orElseThrow(() -> new CityNotFoundException(originCity));
        CityData destination = apiColombiaPort.fetchCityByName(destinationCity)
                .orElseThrow(() -> new CityNotFoundException(destinationCity));

        // 2. Reglas de negocio: determinar la zona
        TariffDomain tariff = new TariffDomain(
                origin.getName(), destination.getName(),
                origin.getDepartmentName(), destination.getDepartmentName(),
                weight);
        Zone zone = tariff.determineZone();

        // 3. Valores de la zona (DynamoDB) y calculo del total
        ZoneTariff rate = tariffRatePort.findByZone(zone)
                .orElseThrow(() -> new TariffNotConfiguredException(zone));
        BigDecimal total = tariff.calculateTotalCost(rate);

        return new TariffResult(
                tariff.getOriginCity(), tariff.getDestinationCity(),
                tariff.getOriginDepartment(), tariff.getDestinationDepartment(),
                weight, zone, total);
    }
}
