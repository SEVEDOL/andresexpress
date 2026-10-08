package com.andresexpress.orders.infrastructure.adapter.out.messaging;

import com.andresexpress.orders.application.port.out.EventPublisherPort;
import com.andresexpress.orders.domain.model.OrderDomain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publica el evento "pedido creado" en una cola SQS (RT-05). Orders ya no espera a que
 * Notifications envie el correo: deja el evento en la cola y responde. Si Notifications
 * esta caido, el evento sigue en la cola y se procesa cuando vuelva.
 *
 * Si SQS falla, lanza la excepcion; CreateOrderService la captura y el pedido queda creado igual (RN-10).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "events.publisher", havingValue = "sqs", matchIfMissing = true)
public class SqsEventPublisherAdapter implements EventPublisherPort {

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private final String queueName;
    private final boolean createQueue;
    private volatile String queueUrl;

    public SqsEventPublisherAdapter(SqsClient sqs,
                                    JsonMapper jsonMapper,
                                    @Value("${aws.sqs.order-created-queue}") String queueName,
                                    @Value("${aws.sqs.create-queue:true}") boolean createQueue) {
        this.sqs = sqs;
        this.jsonMapper = jsonMapper;
        this.queueName = queueName;
        this.createQueue = createQueue;
    }

    @Override
    public void publishOrderCreated(OrderDomain order) {
        sqs.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl())
                .messageBody(jsonMapper.writeValueAsString(OrderCreatedEvent.from(order)))
                .build());
        log.info("Evento OrderCreated publicado en la cola {} para el pedido {}", queueName, order.getOrderId());
    }

    /**
     * URL de la cola, resuelta una sola vez. En local se crea si no existe (CreateQueue es
     * idempotente: si ya existe devuelve su URL); en AWS la cola ya debe estar creada.
     */
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
