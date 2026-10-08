package com.andesexpress.notifications.application.port.in;

public interface SendNotificationUseCase {
    /** @return true si se envio el correo; false si el evento era un duplicado y se descarto (RF-11). */
    boolean processNotification(OrderCreatedEventCommand command);
}
