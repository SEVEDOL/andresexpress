package com.andresexpress.orders.application.service;

import com.andresexpress.orders.application.port.in.GetWaybillUseCase;
import com.andresexpress.orders.application.port.in.RegenerateWaybillUseCase;
import com.andresexpress.orders.application.port.out.WaybillRequestPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegenerateWaybillService implements RegenerateWaybillUseCase {

    private final GetWaybillUseCase getWaybillUseCase;
    private final WaybillRequestPort waybillRequestPort;

    @Override
    public void regenerate(String trackingNumber) {
        OrderDomain order = getWaybillUseCase.getWaybillByTrackingNumber(trackingNumber);

        // En la base solo esta el hash (RN-13): el numero que debe ir impreso en el PDF es el que escribio el cliente
        String plainTrackingNumber = trackingNumber.trim().toUpperCase();
        waybillRequestPort.requestWaybill(order.toBuilder().plainTrackingNumber(plainTrackingNumber).build());
    }
}
