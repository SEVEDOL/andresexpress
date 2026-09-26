package com.andesexpress.notifications.application.port.out;

/**
 * Registro (log) de notificaciones procesadas. Es la base de la idempotencia:
 * un mismo evento (eventId) solo puede registrarse una vez.
 */
public interface NotificationRepositoryPort {

    /**
     * Registra el evento como "en proceso" SOLO si no existia.
     * @return true si se registro; false si el eventId ya estaba (evento duplicado).
     */
    boolean tryRegister(String eventId, String recipientEmail);

    /** Marca el evento como enviado. */
    void markAsSent(String eventId);

    /** Borra el registro para permitir reintentar (se usa cuando el envio del correo falla). */
    void release(String eventId);
}
