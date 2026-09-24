package com.andresexpress.orders.infrastructure.adapter.out.external;

public record CoverageRequest(String originCity, String destinationCity, Double weight) {
}
