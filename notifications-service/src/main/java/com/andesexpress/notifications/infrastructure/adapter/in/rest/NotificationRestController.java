package com.andesexpress.notifications.infrastructure.adapter.in.rest;

import com.andesexpress.notifications.application.port.in.OrderCreatedEventCommand;
import com.andesexpress.notifications.application.port.in.SendNotificationUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final SendNotificationUseCase sendNotificationUseCase;

    @PostMapping("/simulate")
    public ResponseEntity<String> simulateNotification(@RequestBody OrderCreatedEventCommand command) {
        sendNotificationUseCase.processNotification(command);
        return ResponseEntity.ok("Notificación procesada (evaluando idempotencia).");
    }
}