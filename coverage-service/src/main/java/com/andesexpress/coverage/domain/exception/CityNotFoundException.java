package com.andesexpress.coverage.domain.exception;

public class CityNotFoundException extends RuntimeException {
    public CityNotFoundException(String cityName) {
        super("La ciudad especificada no fue encontrada en la API oficial de Colombia: " + cityName);
    }
}