package com.andresexpress.orders.infrastructure.adapter.out.storage;

import com.andresexpress.orders.domain.exception.WaybillUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3WaybillDocumentAdapterTest {

    private static final String HASH = "f2cff04b06d6ee2d0dbc31a807083d653f2de9daed45ebb6d1c20deb41c98406";

    @Mock
    private S3Client s3;

    private S3WaybillDocumentAdapter adapter;

    @BeforeEach
    void setUp() {
        // Firmar una URL es un calculo local: el presigner real funciona sin red, con credenciales de prueba
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("prueba", "prueba")))
                .build();
        adapter = new S3WaybillDocumentAdapter(s3, presigner, "bucket-guias", "guias/", 15);
    }

    @Test
    @DisplayName("si el PDF existe, entrega una URL prefirmada de 15 minutos hacia ese archivo")
    void existingPdfGetsAPresignedUrl() {
        Optional<String> url = adapter.findDownloadUrl(HASH);

        assertThat(url).isPresent();
        assertThat(url.get())
                .contains("bucket-guias")
                .contains("/guias/" + HASH + ".pdf")
                .contains("X-Amz-Expires=900")
                .contains("X-Amz-Signature=");
    }

    @Test
    @DisplayName("si el PDF aun no existe, responde vacio")
    @SuppressWarnings("unchecked")
    void missingPdfIsEmpty() {
        when(s3.headObject(any(Consumer.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).message("Not Found").build());

        assertThat(adapter.findDownloadUrl(HASH)).isEmpty();
    }

    @Test
    @DisplayName("si lo que falta es el bucket, no se confunde con 'la guia aun no esta lista'")
    @SuppressWarnings("unchecked")
    void missingBucketIsUnavailableNotPending() {
        when(s3.headObject(any(Consumer.class)))
                .thenThrow(NoSuchKeyException.builder().statusCode(404).message("Not Found").build());
        when(s3.headBucket(any(Consumer.class)))
                .thenThrow(NoSuchBucketException.builder().statusCode(404).message("Not Found").build());

        assertThatThrownBy(() -> adapter.findDownloadUrl(HASH))
                .isInstanceOf(WaybillUnavailableException.class)
                .hasRootCauseInstanceOf(NoSuchBucketException.class);
    }

    @Test
    @DisplayName("un error de S3 que no es 'no existe' se reporta como no disponible")
    @SuppressWarnings("unchecked")
    void otherS3ErrorsAreUnavailable() {
        when(s3.headObject(any(Consumer.class)))
                .thenThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

        assertThatThrownBy(() -> adapter.findDownloadUrl(HASH)).isInstanceOf(WaybillUnavailableException.class);
    }

    @Test
    @DisplayName("si S3 no responde, se reporta como no disponible")
    @SuppressWarnings("unchecked")
    void networkFailureIsUnavailable() {
        when(s3.headObject(any(Consumer.class))).thenThrow(SdkClientException.create("sin conexion"));

        assertThatThrownBy(() -> adapter.findDownloadUrl(HASH)).isInstanceOf(WaybillUnavailableException.class);
    }
}
