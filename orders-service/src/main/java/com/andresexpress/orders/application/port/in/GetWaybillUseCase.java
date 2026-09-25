package com.andresexpress.orders.application.port.in;

import com.andresexpress.orders.domain.model.OrderDomain;

public interface GetWaybillUseCase {
    OrderDomain getWaybillByTrackingNumber(String trackingNumber);
}
