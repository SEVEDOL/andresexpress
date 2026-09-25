package com.andresexpress.orders.domain.exception;

public class InvalidCityException extends RuntimeException {
    public InvalidCityException(String message) {
        super(message);
    }
}
