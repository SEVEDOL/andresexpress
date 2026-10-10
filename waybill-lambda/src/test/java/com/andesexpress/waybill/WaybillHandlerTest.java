package com.andesexpress.waybill;

import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.andesexpress.waybill.Orders.TRACKING;
import static com.andesexpress.waybill.Orders.TRACKING_HASH;
import static com.andesexpress.waybill.Orders.json;
import static org.assertj.core.api.Assertions.assertThat;

class WaybillHandlerTest {

    private static final String EXPECTED_KEY = "guias/" + TRACKING_HASH + ".pdf";

    /** Bucket en memoria. */
    private static class FakeStorage implements WaybillStorage {
        final Map<String, byte[]> objects = new LinkedHashMap<>();
        boolean fail;

        @Override
        public void save(String key, byte[] pdf) {
            if (fail) {
                throw new IllegalStateException("S3 no disponible");
            }
            objects.put(key, pdf);
        }
    }

    private final FakeStorage storage = new FakeStorage();
    private final WaybillHandler handler = new WaybillHandler(storage, "guias/");

    private static SQSEvent sqsEvent(String... bodies) {
        List<SQSEvent.SQSMessage> records = new ArrayList<>();
        for (int i = 0; i < bodies.length; i++) {
            SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
            message.setMessageId("msg-" + i);
            message.setBody(bodies[i]);
            records.add(message);
        }
        SQSEvent event = new SQSEvent();
        event.setRecords(records);
        return event;
    }

    private static List<String> failedIds(SQSBatchResponse response) {
        return response.getBatchItemFailures().stream().map(SQSBatchResponse.BatchItemFailure::getItemIdentifier).toList();
    }

    @Test
    @DisplayName("RN-13: la clave usa el mismo hash que Orders y no contiene el numero en claro")
    void keyUsesTheSameHashAsOrders() {
        assertThat(WaybillKey.of("guias/", TRACKING)).isEqualTo(EXPECTED_KEY).doesNotContain("12345678");
    }

    @Test
    @DisplayName("la clave normaliza igual que la consulta de guia")
    void keyNormalizesLikeTheWaybillLookup() {
        assertThat(WaybillKey.of("guias/", "  andes-12345678 ")).isEqualTo(EXPECTED_KEY);
    }

    @Test
    @DisplayName("RF-07: guarda el PDF del pedido en el bucket")
    void savesThePdfInTheBucket() {
        SQSBatchResponse response = handler.handleRequest(sqsEvent(json(TRACKING)), null);

        assertThat(failedIds(response)).isEmpty();
        assertThat(storage.objects).containsOnlyKeys(EXPECTED_KEY);
        assertThat(new String(storage.objects.get(EXPECTED_KEY), StandardCharsets.ISO_8859_1)).startsWith("%PDF-");
    }

    @Test
    @DisplayName("un evento duplicado sobrescribe el mismo archivo")
    void duplicatedEventOverwritesTheSameFile() {
        handler.handleRequest(sqsEvent(json(TRACKING), json(TRACKING)), null);

        assertThat(storage.objects).hasSize(1);
    }

    @Test
    @DisplayName("un mensaje invalido no impide procesar los demas del lote")
    void invalidMessageDoesNotBlockTheBatch() {
        SQSBatchResponse response = handler.handleRequest(sqsEvent("esto no es json", json(TRACKING)), null);

        assertThat(failedIds(response)).containsExactly("msg-0");
        assertThat(storage.objects).hasSize(1);
    }

    @Test
    @DisplayName("un evento sin numero de guia se reporta como fallo")
    void eventWithoutTrackingNumberFails() {
        SQSBatchResponse response = handler.handleRequest(sqsEvent(json(null)), null);

        assertThat(failedIds(response)).containsExactly("msg-0");
        assertThat(storage.objects).isEmpty();
    }

    @Test
    @DisplayName("si S3 falla, el mensaje vuelve a la cola")
    void storageFailureSendsTheMessageBackToTheQueue() {
        storage.fail = true;

        SQSBatchResponse response = handler.handleRequest(sqsEvent(json(TRACKING)), null);

        assertThat(failedIds(response)).containsExactly("msg-0");
    }

    @Test
    @DisplayName("acepta el evento envuelto por SNS")
    void acceptsTheEventWrappedBySns() {
        String envelope = "{\"Type\":\"Notification\",\"Message\":" + quote(json(TRACKING)) + "}";

        SQSBatchResponse response = handler.handleRequest(sqsEvent(envelope), null);

        assertThat(failedIds(response)).isEmpty();
        assertThat(storage.objects).containsOnlyKeys(EXPECTED_KEY);
    }

    @Test
    @DisplayName("RN-13: el numero de guia no aparece en el log, ni cuando sale bien ni cuando falla")
    void trackingNumberNeverReachesTheLog() {
        PrintStream original = System.out;
        ByteArrayOutputStream log = new ByteArrayOutputStream();
        System.setOut(new PrintStream(log, true, StandardCharsets.UTF_8));
        try {
            String cutJson = json(TRACKING).substring(0, 140); // JSON roto que alcanza a traer el numero de guia
            handler.handleRequest(sqsEvent(json(TRACKING), cutJson), null);
        } finally {
            System.setOut(original);
        }

        assertThat(log.toString(StandardCharsets.UTF_8)).contains("Guia generada", "No se pudo generar").doesNotContain(TRACKING);
    }

    private static String quote(String json) {
        return "\"" + json.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
