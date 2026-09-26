package com.andesexpress.coverage.domain.model;

import java.math.BigDecimal;

/**
 * Tarifa configurada para una zona (se guarda en DynamoDB, no en el codigo).
 *
 * @param baseTariff  valor base en COP
 * @param includedKg  kilos incluidos en la tarifa base
 * @param extraKgRate valor en COP por cada kg que supere includedKg
 */
public record ZoneTariff(Zone zone, BigDecimal baseTariff, BigDecimal includedKg, BigDecimal extraKgRate) {
}
