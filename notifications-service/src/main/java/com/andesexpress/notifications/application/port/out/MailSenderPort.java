package com.andesexpress.notifications.application.port.out;

public interface MailSenderPort {
    void sendEmail(String to, String subject, String body);
}