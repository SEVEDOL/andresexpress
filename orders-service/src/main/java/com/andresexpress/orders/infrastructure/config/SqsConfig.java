package com.andresexpress.orders.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;
import java.time.Duration;

/**
 * Cliente de SQS.
 * - En AWS (EC2): sin endpoint; las credenciales salen solas del rol de la instancia (LabInstanceProfile).
 * - En local: SQS_ENDPOINT=http://localhost:9324 (ElasticMQ en Docker) con credenciales falsas.
 */
@Configuration
@ConditionalOnProperty(name = "events.publisher", havingValue = "sqs", matchIfMissing = true)
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(@Value("${aws.region}") String region,
                               @Value("${aws.sqs.endpoint:}") String endpoint) {
        SqsClientBuilder builder = SqsClient.builder()
                .region(Region.of(region))
                // Publicar debe ser rapido: si SQS no responde en 3 s, el pedido se guarda igual (RN-10)
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallTimeout(Duration.ofSeconds(3))
                        .build());
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }
}
