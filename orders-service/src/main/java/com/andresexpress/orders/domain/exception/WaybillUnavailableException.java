package com.andresexpress.orders.domain.exception;

/** No se pudo consultar o pedir la guia en PDF porque el almacenamiento o la cola no respondieron. */
public class WaybillUnavailableException extends RuntimeException {
    public WaybillUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
