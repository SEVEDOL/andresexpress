package com.andesexpress.notifications.infrastructure.adapter.out.persistence;

import com.andesexpress.notifications.application.port.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final SpringDataNotificationRepository repository;

    @Override
    public Boolean existsByEventId(String eventId) {
        return repository.existsById(eventId);
    }

    @Override
    public void saveProcessedEvent(String eventId) {
        NotificationEntity entity = NotificationEntity.builder()
                .eventId(eventId)
                .processed(true)
                .build();
        repository.save(entity);
    }
}