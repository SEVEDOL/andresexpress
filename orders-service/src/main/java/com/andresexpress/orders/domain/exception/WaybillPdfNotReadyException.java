package com.andresexpress.orders.domain.exception;

/** El pedido existe, pero su guia en PDF todavia no se ha generado. */
public class WaybillPdfNotReadyException extends RuntimeException {
    public WaybillPdfNotReadyException(String message) {
        super(message);
    }
}
