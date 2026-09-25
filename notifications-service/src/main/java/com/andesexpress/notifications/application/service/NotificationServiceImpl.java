package com.andesexpress.notifications.application.service;

import com.andesexpress.notifications.application.port.in.OrderCreatedEventCommand;
import com.andesexpress.notifications.application.port.in.SendNotificationUseCase;
import com.andesexpress.notifications.application.port.out.MailSenderPort;
import com.andesexpress.notifications.application.port.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements SendNotificationUseCase {

    private final NotificationRepositoryPort notificationRepositoryPort;
    private final MailSenderPort mailSenderPort;

    @Override
    @Transactional
    public void processNotification(OrderCreatedEventCommand command) {
        // Regla de Idempotencia
        if (notificationRepositoryPort.existsByEventId(command.getEventId())) {
            log.warn("El evento con ID {} ya fue procesado previamente. Descartando notificación duplicada.", command.getEventId());
            return;
        }

        log.info("Procesando notificación para el evento ID: {}", command.getEventId());

        // Enviar correo
        String subject = "Confirmación de Envío - Andes Express";
        String body = buildEmailBody(command);

        mailSenderPort.sendEmail(command.getSenderEmail(), subject, body);

        // Registrar como procesado
        notificationRepositoryPort.saveProcessedEvent(command.getEventId());
        log.info("Evento ID {} registrado exitosamente en la base de datos de auditoría.", command.getEventId());
    }

    /** Cuerpo del correo con la misma informacion de la guia (RF-10). */
    private String buildEmailBody(OrderCreatedEventCommand c) {
        return "Hola " + valueOrDash(c.getSenderName()) + ",\n\n"
                + "Tu pedido ha sido creado exitosamente.\n\n"
                + "Numero de guia: " + valueOrDash(c.getPlainTrackingNumber()) + "\n"
                + "Origen: " + valueOrDash(c.getOriginCity()) + "\n"
                + "Destino: " + valueOrDash(c.getDestinationCity()) + "\n"
                + "Peso (kg): " + valueOrDash(c.getWeight()) + "\n"
                + "Tipo de envio: " + valueOrDash(c.getShipmentType()) + "\n"
                + "Tarifa (COP): " + valueOrDash(c.getTotalTariff()) + "\n"
                + "Remitente: " + valueOrDash(c.getSenderName()) + " - " + valueOrDash(c.getSenderPhone()) + "\n"
                + "Destinatario: " + valueOrDash(c.getRecipientName()) + " - " + valueOrDash(c.getRecipientPhone()) + "\n\n"
                + "Si no recibes este correo, puedes consultar tu guia en la plataforma con su numero.\n\n"
                + "Gracias por confiar en Andes Express.";
    }

    private String valueOrDash(Object value) {
        return value == null ? "-" : value.toString();
    }
}
