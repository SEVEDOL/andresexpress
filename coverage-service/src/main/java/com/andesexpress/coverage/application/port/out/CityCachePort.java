package com.andesexpress.coverage.application.port.out;


import com.andesexpress.coverage.domain.model.CityData;
import java.util.Optional;

public interface CityCachePort {
    Optional<CityData> findByName(String cityName);
    void save(CityData cityData);
}