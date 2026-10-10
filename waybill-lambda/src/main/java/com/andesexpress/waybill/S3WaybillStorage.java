package com.andesexpress.waybill;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/** Guarda las guias en un bucket S3. Las credenciales salen del rol de ejecucion de la Lambda. */
public class S3WaybillStorage implements WaybillStorage {

    private final S3Client s3;
    private final String bucket;

    public S3WaybillStorage(String bucket) {
        this(S3Client.builder().httpClient(UrlConnectionHttpClient.create()).build(), bucket);
    }

    S3WaybillStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void save(String key, byte[] pdf) {
        s3.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdf));
    }
}
