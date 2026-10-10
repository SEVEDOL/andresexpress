package com.andresexpress.orders.infrastructure.adapter.out.storage;

import com.andresexpress.orders.application.port.out.WaybillDocumentPort;
import com.andresexpress.orders.domain.exception.WaybillUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.util.Optional;

/**
 * Las guias en PDF viven en un bucket S3 privado. Orders no entrega el archivo: entrega una
 * URL prefirmada que deja descargarlo durante unos minutos.
 * El nombre del archivo es el hash del numero de guia, el mismo que usa la Lambda (RN-13).
 */
@Component
public class S3WaybillDocumentAdapter implements WaybillDocumentPort {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final String prefix;
    private final Duration urlDuration;

    public S3WaybillDocumentAdapter(S3Client s3,
                                    S3Presigner presigner,
                                    @Value("${aws.s3.waybill-bucket}") String bucket,
                                    @Value("${aws.s3.waybill-prefix}") String prefix,
                                    @Value("${aws.s3.url-minutes}") long urlMinutes) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.prefix = prefix;
        this.urlDuration = Duration.ofMinutes(urlMinutes);
    }

    @Override
    public Optional<String> findDownloadUrl(String hashedTrackingNumber) {
        String key = prefix + hashedTrackingNumber + ".pdf";

        // Una URL prefirmada se puede crear aunque el archivo no exista: por eso se comprueba primero
        try {
            s3.headObject(head -> head.bucket(bucket).key(key));
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                requireBucket();
                return Optional.empty();
            }
            throw unavailable(e);
        } catch (SdkException e) {
            throw unavailable(e);
        }

        return Optional.of(presigner.presignGetObject(presign -> presign
                        .signatureDuration(urlDuration)
                        .getObjectRequest(get -> get.bucket(bucket).key(key)))
                .url().toString());
    }

    /**
     * S3 responde el mismo 404 cuando falta el archivo y cuando falta el bucket. Sin esta comprobacion,
     * un bucket mal configurado se veria para siempre como "la guia todavia no esta lista".
     */
    private void requireBucket() {
        try {
            s3.headBucket(head -> head.bucket(bucket));
        } catch (SdkException e) {
            throw new WaybillUnavailableException(
                    "No se pudo consultar la guía en PDF en este momento", new IllegalStateException(
                    "El bucket de guias '" + bucket + "' no existe o no es accesible (revisar WAYBILL_BUCKET)", e));
        }
    }

    private static WaybillUnavailableException unavailable(SdkException e) {
        return new WaybillUnavailableException("No se pudo consultar la guía en PDF en este momento", e);
    }
}
