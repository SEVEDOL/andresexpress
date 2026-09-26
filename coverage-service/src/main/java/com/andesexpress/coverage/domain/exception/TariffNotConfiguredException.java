package com.andesexpress.coverage.domain.exception;

import com.andesexpress.coverage.domain.model.Zone;

/** No hay tarifa configurada para la zona (falta el registro en la tabla de tarifas). */
public class TariffNotConfiguredException extends RuntimeException {
    public TariffNotConfiguredException(Zone zone) {
        super("No hay tarifa configurada para la zona " + zone);
    }
}
