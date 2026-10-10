package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.application.port.out.WaybillRequestPort;
import com.andresexpress.orders.domain.exception.WaybillUnavailableException;
import com.andresexpress.orders.domain.model.OrderDomain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Con events.publisher=http no hay cola, y sin cola no hay quien dispare la Lambda de la guia. */
@Component
@ConditionalOnProperty(name = "events.publisher", havingValue = "http")
public class NoQueueWaybillRequestAdapter implements WaybillRequestPort {

    @Override
    public void requestWaybill(OrderDomain order) {
        throw new WaybillUnavailableException(
                "La guía en PDF no está disponible en este entorno (requiere events.publisher=sqs)", null);
    }
}
