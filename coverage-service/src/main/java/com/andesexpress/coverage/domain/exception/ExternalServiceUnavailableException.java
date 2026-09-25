package com.andesexpress.coverage.domain.exception;

/**
 * La fuente externa (API Colombia) no respondio: timeout, caida o error del servidor.
 * Es distinto de CityNotFoundException: aqui NO sabemos si la ciudad existe.
 */
public class ExternalServiceUnavailableException extends RuntimeException {
    public ExternalServiceUnavailableException(String message) {
        super(message);
    }
}
