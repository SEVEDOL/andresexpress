package com.andesexpress.waybill;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

/**
 * Nombre del PDF dentro del bucket. Usa el mismo hash que Orders (SHA-256 del numero en mayusculas
 * y sin espacios): Orders ubica el PDF a partir del numero que escribe el cliente, sin guardar ese
 * numero en claro (RN-13). Como la clave es siempre la misma, un evento duplicado sobrescribe el mismo archivo.
 */
public final class WaybillKey {

    private WaybillKey() {
    }

    public static String of(String prefix, String plainTrackingNumber) {
        String normalized = plainTrackingNumber.trim().toUpperCase(Locale.ROOT);
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(normalized.getBytes(StandardCharsets.UTF_8));
            return prefix + HexFormat.of().formatHex(hash) + ".pdf";
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
