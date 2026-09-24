package com.andesexpress.coverage.application.port.in;

import com.andesexpress.coverage.domain.model.TariffDomain;

public interface ValidateCoverageUseCase {
    TariffDomain validateAndCalculate(String originCity, String destinationCity, Double weight);
}