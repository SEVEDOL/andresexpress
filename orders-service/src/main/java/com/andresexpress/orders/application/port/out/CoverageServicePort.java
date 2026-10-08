package com.andresexpress.orders.application.port.out;

import com.andresexpress.orders.domain.model.TariffResult;

public interface CoverageServicePort {
    TariffResult validateAndCalculateTariff(String originCity, String originDepartment,
                                            String destinationCity, String destinationDepartment,
                                            Double weight);
}
