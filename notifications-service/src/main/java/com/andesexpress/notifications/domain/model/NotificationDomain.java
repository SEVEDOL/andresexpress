package com.andesexpress.notifications.domain.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NotificationDomain {
    private String eventId;
    private String recipientEmail;
    private String trackingNumber;
    private Boolean processed;
}