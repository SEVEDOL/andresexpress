package com.andresexpress.orders.application.port.out;

import com.andresexpress.orders.domain.model.TariffResult;

public interface CoverageServicePort {
    TariffResult validateAndCalculateTariff(String originCity, String destinationCity, Double weight);
}
