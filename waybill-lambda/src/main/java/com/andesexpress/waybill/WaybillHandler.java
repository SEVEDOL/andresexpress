package com.andesexpress.waybill;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Lambda que genera la guia en PDF y la guarda en S3 (RF-07, RT-05).
 *
 * Disparador: cola SQS con el evento "pedido creado" que publica Orders.
 * Variables de entorno: WAYBILL_BUCKET (obligatoria) y WAYBILL_PREFIX (por defecto "guias/").
 * Handler: com.andesexpress.waybill.WaybillHandler::handleRequest
 */
public class WaybillHandler implements RequestHandler<SQSEvent, SQSBatchResponse> {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final WaybillStorage storage;
    private final String prefix;
    private final WaybillPdfGenerator generator = new WaybillPdfGenerator();

    /** Constructor que usa Lambda. */
    public WaybillHandler() {
        this(new S3WaybillStorage(requiredEnv("WAYBILL_BUCKET")),
                System.getenv().getOrDefault("WAYBILL_PREFIX", "guias/"));
    }

    WaybillHandler(WaybillStorage storage, String prefix) {
        this.storage = storage;
        this.prefix = prefix;
    }

    /**
     * Procesa cada mensaje por separado: si uno falla, solo ese vuelve a la cola
     * (y tras varios intentos, a la DLQ). Los demas del lote no se repiten.
     */
    @Override
    public SQSBatchResponse handleRequest(SQSEvent event, Context context) {
        List<SQSBatchResponse.BatchItemFailure> failures = new ArrayList<>();
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                OrderCreatedEvent order = parse(message.getBody());
                String key = WaybillKey.of(prefix, requiredTrackingNumber(order));
                storage.save(key, generator.generate(order));
                // El numero de guia no se escribe en el log: solo se guarda protegido con hash (RN-13)
                System.out.println("Guia generada para el pedido " + order.eventId() + " en " + key);
            } catch (Exception e) {
                // Los errores de lectura del JSON citan el contenido del mensaje, que trae el numero de guia:
                // de esos solo se registra el tipo
                String detail = e instanceof IllegalArgumentException ? e.getMessage() : e.getClass().getSimpleName();
                System.out.println("No se pudo generar la guia del mensaje " + message.getMessageId() + ": " + detail);
                failures.add(new SQSBatchResponse.BatchItemFailure(message.getMessageId()));
            }
        }
        return new SQSBatchResponse(failures);
    }

    private static OrderCreatedEvent parse(String body) throws Exception {
        JsonNode node = JSON.readTree(body);
        // Si la cola esta suscrita a un tema SNS, el evento viene dentro de "Message"
        if (node.path("Type").asText().equals("Notification") && node.hasNonNull("Message")) {
            node = JSON.readTree(node.get("Message").asText());
        }
        if (!node.isObject()) {
            throw new IllegalArgumentException("El mensaje no es un evento de pedido");
        }
        return JSON.treeToValue(node, OrderCreatedEvent.class);
    }

    private static String requiredTrackingNumber(OrderCreatedEvent order) {
        if (order.plainTrackingNumber() == null || order.plainTrackingNumber().isBlank()) {
            throw new IllegalArgumentException("El evento no trae numero de guia");
        }
        return order.plainTrackingNumber();
    }

    private static String requiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Falta la variable de entorno " + name);
        }
        return value;
    }
}
