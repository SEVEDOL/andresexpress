package com.andesexpress.coverage.application.port.out;

import com.andesexpress.coverage.domain.model.Zone;
import com.andesexpress.coverage.domain.model.ZoneTariff;

import java.util.Optional;

/** Puerto de salida: de donde se leen las tarifas por zona (implementado con DynamoDB). */
public interface TariffRatePort {
    Optional<ZoneTariff> findByZone(Zone zone);
}
