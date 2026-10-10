package com.andresexpress.orders.application.port.out;

import com.andresexpress.orders.domain.model.OrderDomain;

/** Puerto de salida: pide que se genere la guia en PDF de un pedido (lo atiende la Lambda, RF-07). */
public interface WaybillRequestPort {
    void requestWaybill(OrderDomain order);
}
