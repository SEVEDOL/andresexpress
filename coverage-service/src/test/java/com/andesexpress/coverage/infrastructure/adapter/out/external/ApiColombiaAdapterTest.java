package com.andesexpress.coverage.infrastructure.adapter.out.external;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiColombiaAdapterTest {

    @Test
    void nombreParcialNoCoincide() {
        assertThat(ApiColombiaAdapter.matchesCityName("San Francisco", "San")).isFalse();
        assertThat(ApiColombiaAdapter.matchesCityName("Santa Marta", "Santa")).isFalse();
    }

    @Test
    void nombreCompletoCoincideSinTildesNiMayusculas() {
        assertThat(ApiColombiaAdapter.matchesCityName("Medellín", "medellin")).isTrue();
        assertThat(ApiColombiaAdapter.matchesCityName("Bogotá D.C.", "BOGOTÁ")).isTrue();
        assertThat(ApiColombiaAdapter.matchesCityName("San Francisco", "  san   FRANCISCO ")).isTrue();
    }

    @Test
    void bogotaCoincideConOSinSufijoDistritoCapital() {
        assertThat(ApiColombiaAdapter.matchesCityName("Bogotá D.C.", "Bogota")).isTrue();
        assertThat(ApiColombiaAdapter.matchesCityName("Bogotá D.C.", "Bogotá D.C.")).isTrue();
        assertThat(ApiColombiaAdapter.matchesCityName("Bogotá D.C.", "Bogota, DC")).isTrue();
    }

    @Test
    void nombreVacioONuloNoCoincide() {
        assertThat(ApiColombiaAdapter.matchesCityName("Tunja", "")).isFalse();
        assertThat(ApiColombiaAdapter.matchesCityName("Tunja", null)).isFalse();
    }
}
