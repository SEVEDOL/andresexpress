package com.andesexpress.notifications.infrastructure.adapter.out.messaging;

import com.andesexpress.notifications.application.port.out.MailSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JavaMailAdapter implements MailSenderPort {

    private final JavaMailSender mailSender;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Correo electrónico enviado exitosamente a: {}", to);
        } catch (Exception e) {
            log.error("Error al enviar el correo electrónico a {}: {}", to, e.getMessage());
            throw new RuntimeException("Fallo en el envío de correo", e);
        }
    }
}