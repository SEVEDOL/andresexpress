package com.andesexpress.notifications.infrastructure.adapter.in.messaging;

import com.andesexpress.notifications.application.port.in.OrderCreatedEventCommand;
import com.andesexpress.notifications.application.port.in.SendNotificationUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Adaptador de entrada: lee los eventos "pedido creado" de la cola SQS (RT-05) y los pasa al caso de uso.
 *
 * - Correo enviado o evento duplicado -> se borra el mensaje de la cola.
 * - Fallo al enviar (Gmail caido)     -> NO se borra: SQS lo vuelve a entregar al vencer la visibilidad.
 *   Tras varios intentos fallidos, SQS lo mueve a la cola de mensajes fallidos (DLQ).
 * - Mensaje invalido (JSON roto, sin eventId) -> se borra, porque reintentarlo nunca va a funcionar.
 *
 * Reintentar es seguro porque el caso de uso es idempotente por eventId (RF-11).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "aws.sqs.listener-enabled", havingValue = "true", matchIfMissing = true)
public class OrderCreatedSqsListener implements SmartLifecycle {

    private static final int WAIT_TIME_SECONDS = 20;       // long polling: no gasta peticiones con la cola vacia
    private static final String VISIBILITY_TIMEOUT = "60";   // > ~12 s que tarda Gmail en enviar un correo
    private static final String MAX_RECEIVE_COUNT = "5";     // intentos antes de mandar el mensaje a la DLQ

    private final SqsClient sqs;
    private final JsonMapper jsonMapper;
    private final SendNotificationUseCase sendNotificationUseCase;
    private final String queueName;
    private final boolean createQueue;
    private final int concurrency;

    private volatile boolean running;
    private Thread poller;
    private ExecutorService workers;

    public OrderCreatedSqsListener(SqsClient sqs,
                                   JsonMapper jsonMapper,
                                   SendNotificationUseCase sendNotificationUseCase,
                                   @Value("${aws.sqs.order-created-queue}") String queueName,
                                   @Value("${aws.sqs.create-queue:true}") boolean createQueue,
                                   @Value("${aws.sqs.concurrency:5}") int concurrency) {
        this.sqs = sqs;
        this.jsonMapper = jsonMapper;
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.queueName = queueName;
        this.createQueue = createQueue;
        this.concurrency = concurrency;
    }

    @Override
    public void start() {
        running = true;
        workers = Executors.newFixedThreadPool(concurrency);
        poller = new Thread(this::pollLoop, "sqs-" + queueName);
        poller.setDaemon(true);
        poller.start();
    }

    @Override
    public void stop() {
        running = false;
        if (poller != null) {
            poller.interrupt();
        }
        if (workers != null) {
            workers.shutdown();
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private void pollLoop() {
        String queueUrl = null;
        while (running) {
            try {
                if (queueUrl == null) {
                    queueUrl = resolveQueueUrl();
                    log.info("Escuchando eventos de la cola SQS {}", queueName);
                }
                List<Message> messages = sqs.receiveMessage(ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(Math.min(concurrency, 10))
                        .waitTimeSeconds(WAIT_TIME_SECONDS)
                        .build()).messages();

                // Los correos del lote se envian en paralelo (Gmail tarda ~11 s cada uno)
                String url = queueUrl;
                CompletableFuture.allOf(messages.stream()
                        .map(m -> CompletableFuture.runAsync(() -> handle(url, m), workers))
                        .toArray(CompletableFuture[]::new)).join();
            } catch (Exception e) {
                if (!running) {
                    return;
                }
                log.error("Error leyendo la cola SQS {}; se reintenta en 5 s: {}", queueName, e.getMessage());
                sleepQuietly(5_000);
            }
        }
    }

    void handle(String queueUrl, Message message) {
        OrderCreatedEventCommand command;
        try {
            command = jsonMapper.readValue(message.body(), OrderCreatedEventCommand.class);
        } catch (JacksonException e) {
            log.error("Mensaje {} con JSON invalido; se descarta: {}", message.messageId(), e.getOriginalMessage());
            delete(queueUrl, message);
            return;
        }

        try {
            sendNotificationUseCase.processNotification(command);
            delete(queueUrl, message);
        } catch (IllegalArgumentException e) {
            log.error("Evento {} invalido; se descarta: {}", message.messageId(), e.getMessage());
            delete(queueUrl, message);
        } catch (RuntimeException e) {
            // Sin borrar: SQS lo entrega de nuevo cuando vence la visibilidad
            log.warn("No se pudo notificar el evento {}; se reintentara: {}", command.getEventId(), e.getMessage());
        }
    }

    private void delete(String queueUrl, Message message) {
        sqs.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build());
    }

    /**
     * Con create-queue=true crea la cola y su DLQ si no existen y les aplica la configuracion.
     * La cola se crea solo por nombre (idempotente, igual que en Orders, que puede crearla primero)
     * y despues se ajustan sus atributos. Con create-queue=false la cola ya debe existir.
     */
    private String resolveQueueUrl() {
        if (!createQueue) {
            return sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).queueUrl();
        }
        String dlqUrl = sqs.createQueue(CreateQueueRequest.builder().queueName(queueName + "-dlq").build()).queueUrl();
        String dlqArn = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
                        .queueUrl(dlqUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build())
                .attributes().get(QueueAttributeName.QUEUE_ARN);
        String url = sqs.createQueue(CreateQueueRequest.builder().queueName(queueName).build()).queueUrl();
        sqs.setQueueAttributes(SetQueueAttributesRequest.builder()
                .queueUrl(url)
                .attributes(Map.of(
                        QueueAttributeName.VISIBILITY_TIMEOUT, VISIBILITY_TIMEOUT,
                        QueueAttributeName.REDRIVE_POLICY,
                        "{\"deadLetterTargetArn\":\"" + dlqArn + "\",\"maxReceiveCount\":\"" + MAX_RECEIVE_COUNT + "\"}"))
                .build());
        return url;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
