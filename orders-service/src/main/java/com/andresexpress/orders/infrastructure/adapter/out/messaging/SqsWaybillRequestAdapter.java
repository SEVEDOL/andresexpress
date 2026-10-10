package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.application.port.out.WaybillRequestPort;
import com.andresexpress.orders.domain.exception.WaybillUnavailableException;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pide la guia en PDF dejando el evento en la cola que dispara la Lambda (RF-07, RT-05).
 * Es una cola distinta a la de Notifications: en SQS cada mensaje lo recibe un solo consumidor,
 * asi que compartir la cola haria que la Lambda y Notifications se quitaran los mensajes.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "events.publisher", havingValue = "sqs", matchIfMissing = true)
public class SqsWaybillRequestAdapter implements WaybillRequestPort {

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private final String queueName;
    private final boolean createQueue;
    private volatile String queueUrl;

    public SqsWaybillRequestAdapter(SqsClient sqs,
                                    JsonMapper jsonMapper,
                                    @Value("${aws.sqs.waybill-queue}") String queueName,
                                    @Value("${aws.sqs.create-queue:true}") boolean createQueue) {
        this.sqs = sqs;
        this.jsonMapper = jsonMapper;
        this.queueName = queueName;
        this.createQueue = createQueue;
    }

    @Override
    public void requestWaybill(OrderDomain order) {
        try {
            sqs.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl())
                    .messageBody(jsonMapper.writeValueAsString(OrderCreatedEvent.from(order)))
                    .build());
        } catch (SdkException e) {
            throw new WaybillUnavailableException("No se pudo solicitar la guía en PDF en este momento", e);
        }
        log.info("Guia en PDF solicitada en la cola {} para el pedido {}", queueName, order.getOrderId());
    }

    /** URL de la cola, resuelta una sola vez. En local se crea si no existe; en AWS ya debe estar creada. */
    private String queueUrl() {
        String url = queueUrl;
        if (url == null) {
            url = createQueue
                    ? sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl()
                    : sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
            queueUrl = url;
        }
        return url;
    }
}
