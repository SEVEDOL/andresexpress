package com.andresexpress.orders.infrastructure.adapter.out.external;

public record CoverageRequest(String originCity, String originDepartment,
                              String destinationCity, String destinationDepartment,
                              Double weight) {
}
