package com.andesexpress.notifications.infrastructure.adapter.in.rest;

import com.andesexpress.notifications.application.port.in.OrderCreatedEventCommand;
import com.andesexpress.notifications.application.port.in.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final SendNotificationUseCase sendNotificationUseCase;

    /**
     * Ambos casos responden 200: un duplicado no es un error para quien publica el evento
     * (idempotencia), pero el cuerpo indica si el correo se envio o se descarto (RF-11).
     */
    @PostMapping("/simulate")
    public ResponseEntity<Map<String, String>> simulateNotification(@RequestBody OrderCreatedEventCommand command) {
        boolean sent = sendNotificationUseCase.processNotification(command);
        if (sent) {
            return ResponseEntity.ok(Map.of(
                    "status", "SENT",
                    "message", "Notificación enviada."));
        }
        return ResponseEntity.ok(Map.of(
                "status", "DUPLICATE",
                "message", "El evento " + command.getEventId() + " ya fue procesado; se descartó el duplicado."));
    }
}
