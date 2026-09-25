package com.andresexpress.orders.application.service;

import com.andresexpress.orders.application.port.in.GetWaybillUseCase;
import com.andresexpress.orders.application.port.out.OrderRepositoryPort;
import com.andresexpress.orders.domain.exception.OrderNotFoundException;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetWaybillService implements GetWaybillUseCase {

    private final OrderRepositoryPort orderRepositoryPort;

    @Override
    public OrderDomain getWaybillByTrackingNumber(String trackingNumber) {
        String normalized = trackingNumber.trim().toUpperCase();
        String hash = OrderDomain.calculateHash(normalized);
        return orderRepositoryPort.findByHashedTracking(hash)
                .orElseThrow(() -> new OrderNotFoundException("No existe una guía con el número " + normalized));
    }
}
