package com.andesexpress.coverage.application.port.out;

import com.andesexpress.coverage.domain.model.CityData;
import java.util.Optional;

public interface ApiColombiaPort {
    /**
     * Busca el municipio dentro del departamento indicado (RF-04): un mismo nombre
     * puede existir en varios departamentos (ej. Rionegro en Antioquia y Santander).
     */
    Optional<CityData> fetchCity(String cityName, String departmentName);
}
