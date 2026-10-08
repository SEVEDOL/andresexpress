package com.andesexpress.coverage.infrastructure.adapter.out.external;

import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaCityResponseDTO;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaDepartmentResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiColombiaAdapterTest {

    private static final String CITIES_URL = "http://api/City";
    private static final String DEPARTMENTS_URL = "http://api/Department";

    private RestTemplate restTemplate;
    private ApiColombiaAdapter adapter;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        when(restTemplate.getForObject(eq(CITIES_URL), eq(ApiColombiaCityResponseDTO[].class)))
                .thenReturn(new ApiColombiaCityResponseDTO[]{
                        city(1, "Rionegro", 2),
                        city(2, "Rionegro", 27),
                        city(3, "San Francisco", 2),
                        city(4, "Bogotá D.C.", 4)});
        when(restTemplate.getForObject(eq(DEPARTMENTS_URL), eq(ApiColombiaDepartmentResponseDTO[].class)))
                .thenReturn(new ApiColombiaDepartmentResponseDTO[]{
                        department(2, "Antioquia"),
                        department(27, "Santander"),
                        department(4, "Bogotá")});
        adapter = new ApiColombiaAdapter(restTemplate, CITIES_URL, DEPARTMENTS_URL, 60);
    }

    @Test
    void municipioRepetidoSeResuelvePorDepartamento() {
        assertThat(adapter.fetchCity("Rionegro", "Santander"))
                .map(CityData::getDepartmentName).contains("Santander");
        assertThat(adapter.fetchCity("rionegro", "ANTIOQUIA"))
                .map(CityData::getDepartmentName).contains("Antioquia");
    }

    @Test
    void municipioQueNoEstaEnElDepartamentoNoSeEncuentra() {
        assertThat(adapter.fetchCity("San Francisco", "Santander")).isEmpty();
        assertThat(adapter.fetchCity("Rionegro", "Departamento inventado")).isEmpty();
    }

    @Test
    void devuelveLosNombresOficiales() {
        Optional<CityData> bogota = adapter.fetchCity("bogota", "Bogota D.C.");
        assertThat(bogota).map(CityData::getName).contains("Bogotá D.C.");
        assertThat(bogota).map(CityData::getDepartmentName).contains("Bogotá");
    }

    @Test
    void lasListasSeDescarganUnaSolaVez() {
        adapter.fetchCity("Rionegro", "Santander");
        adapter.fetchCity("San Francisco", "Antioquia");
        verify(restTemplate, times(1)).getForObject(eq(CITIES_URL), any());
        verify(restTemplate, times(1)).getForObject(eq(DEPARTMENTS_URL), any());
    }

    @Test
    void nombreParcialNoCoincide() {
        assertThat(ApiColombiaAdapter.matchesName("San Francisco", "San")).isFalse();
        assertThat(ApiColombiaAdapter.matchesName("Santa Marta", "Santa")).isFalse();
    }

    @Test
    void nombreCompletoCoincideSinTildesNiMayusculas() {
        assertThat(ApiColombiaAdapter.matchesName("Medellín", "medellin")).isTrue();
        assertThat(ApiColombiaAdapter.matchesName("Bogotá D.C.", "BOGOTÁ")).isTrue();
        assertThat(ApiColombiaAdapter.matchesName("San Francisco", "  san   FRANCISCO ")).isTrue();
    }

    @Test
    void bogotaCoincideConOSinSufijoDistritoCapital() {
        assertThat(ApiColombiaAdapter.matchesName("Bogotá D.C.", "Bogota")).isTrue();
        assertThat(ApiColombiaAdapter.matchesName("Bogotá D.C.", "Bogotá D.C.")).isTrue();
        assertThat(ApiColombiaAdapter.matchesName("Bogotá D.C.", "Bogota, DC")).isTrue();
    }

    @Test
    void nombreVacioONuloNoCoincide() {
        assertThat(ApiColombiaAdapter.matchesName("Tunja", "")).isFalse();
        assertThat(ApiColombiaAdapter.matchesName("Tunja", null)).isFalse();
    }

    private static ApiColombiaCityResponseDTO city(int id, String name, int departmentId) {
        ApiColombiaCityResponseDTO dto = new ApiColombiaCityResponseDTO();
        dto.setId(id);
        dto.setName(name);
        dto.setDepartmentId(departmentId);
        return dto;
    }

    private static ApiColombiaDepartmentResponseDTO department(int id, String name) {
        ApiColombiaDepartmentResponseDTO dto = new ApiColombiaDepartmentResponseDTO();
        dto.setId(id);
        dto.setName(name);
        return dto;
    }
}
