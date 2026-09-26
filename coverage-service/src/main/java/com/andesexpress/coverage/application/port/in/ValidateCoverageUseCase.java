package com.andesexpress.coverage.application.port.in;

import com.andesexpress.coverage.domain.model.TariffResult;

public interface ValidateCoverageUseCase {
    TariffResult validateAndCalculate(String originCity, String destinationCity, Double weight);
}
