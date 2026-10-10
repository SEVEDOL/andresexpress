package com.andresexpress.orders.application.port.in;

public interface RegenerateWaybillUseCase {

    /** Vuelve a pedir la generacion de la guia en PDF de un pedido que ya existe (RF-09). */
    void regenerate(String trackingNumber);
}
