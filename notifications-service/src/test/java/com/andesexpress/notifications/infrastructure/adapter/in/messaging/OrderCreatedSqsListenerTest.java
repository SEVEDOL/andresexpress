package com.andesexpress.notifications.infrastructure.adapter.in.messaging;

import com.andesexpress.notifications.application.port.in.OrderCreatedEventCommand;
import com.andesexpress.notifications.application.port.in.SendNotificationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderCreatedSqsListenerTest {

    private static final String QUEUE_URL = "http://sqs/order-created";

    private SqsClient sqs;
    private SendNotificationUseCase useCase;
    private OrderCreatedSqsListener listener;

    @BeforeEach
    void setUp() {
        sqs = mock(SqsClient.class);
        useCase = mock(SendNotificationUseCase.class);
        listener = new OrderCreatedSqsListener(sqs, JsonMapper.builder().build(), useCase, "order-created", true, 1);
    }

    private static Message message(String body) {
        return Message.builder().messageId("m-1").receiptHandle("rh-1").body(body).build();
    }

    @Test
    void eventoProcesadoSeBorraDeLaCola() {
        when(useCase.processNotification(any())).thenReturn(true);

        listener.handle(QUEUE_URL, message(
                "{\"eventId\":\"e-1\",\"senderEmail\":\"a@b.co\",\"destinationDepartment\":\"Santander\",\"campoNuevo\":1}"));

        ArgumentCaptor<OrderCreatedEventCommand> command = ArgumentCaptor.forClass(OrderCreatedEventCommand.class);
        verify(useCase).processNotification(command.capture());
        assertThat(command.getValue().getEventId()).isEqualTo("e-1");
        assertThat(command.getValue().getDestinationDepartment()).isEqualTo("Santander");
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void eventoDuplicadoTambienSeBorra() {
        when(useCase.processNotification(any())).thenReturn(false);
        listener.handle(QUEUE_URL, message("{\"eventId\":\"e-1\"}"));
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void siFallaElCorreoNoSeBorraParaQueSqsLoReintente() {
        doThrow(new RuntimeException("Fallo en el envío de correo")).when(useCase).processNotification(any());
        listener.handle(QUEUE_URL, message("{\"eventId\":\"e-1\"}"));
        verify(sqs, never()).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void mensajeInvalidoSeDescarta() {
        listener.handle(QUEUE_URL, message("{roto"));
        verify(useCase, never()).processNotification(any());
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }

    @Test
    void eventoSinEventIdSeDescarta() {
        doThrow(new IllegalArgumentException("El evento no trae eventId")).when(useCase).processNotification(any());
        listener.handle(QUEUE_URL, message("{\"senderEmail\":\"a@b.co\"}"));
        verify(sqs).deleteMessage(any(DeleteMessageRequest.class));
    }
}
