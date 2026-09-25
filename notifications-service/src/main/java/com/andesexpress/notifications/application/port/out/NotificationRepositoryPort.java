package com.andesexpress.notifications.application.port.out;

public interface NotificationRepositoryPort {
    Boolean existsByEventId(String eventId);
    void saveProcessedEvent(String eventId);
}