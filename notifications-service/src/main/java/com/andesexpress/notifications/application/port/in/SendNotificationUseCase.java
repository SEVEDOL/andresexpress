package com.andesexpress.notifications.application.port.in;

public interface SendNotificationUseCase {
    void processNotification(OrderCreatedEventCommand command);
}