package com.andresexpress.orders.domain.exception;

public class CoverageUnavailableException extends RuntimeException {
    public CoverageUnavailableException(String message) {
        super(message);
    }
}
