package com.andesexpress.coverage.application.port.out;

import com.andesexpress.coverage.domain.model.CityData;
import java.util.Optional;

public interface ApiColombiaPort {
    Optional<CityData> fetchCityByName(String cityName);
}