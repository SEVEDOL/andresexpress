package com.andesexpress.coverage.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TariffDomainZoneTest {

    @Test
    void mismoNombreEnOtroDepartamentoNoEsMismaCiudad() {
        TariffDomain tariff = new TariffDomain("Rionegro", "Rionegro", "Antioquia", "Santander", 1.0);
        assertThat(tariff.determineZone()).isEqualTo(Zone.REST_OF_COUNTRY);
    }

    @Test
    void mismaCiudadYDepartamentoEsMismaCiudad() {
        TariffDomain tariff = new TariffDomain("Rionegro", "Rionegro", "Antioquia", "Antioquia", 1.0);
        assertThat(tariff.determineZone()).isEqualTo(Zone.SAME_CITY);
    }
}
