package com.andesexpress.notifications.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;

import java.net.URI;

/**
 * Cliente de SQS.
 * - En AWS (EC2): sin endpoint; las credenciales salen solas del rol de la instancia (LabInstanceProfile).
 * - En local: SQS_ENDPOINT=http://localhost:9324 (ElasticMQ en Docker) con credenciales falsas.
 */
@Configuration
@ConditionalOnProperty(name = "aws.sqs.listener-enabled", havingValue = "true", matchIfMissing = true)
public class SqsConfig {

    @Bean
    public SqsClient sqsClient(@Value("${aws.region}") String region,
                               @Value("${aws.sqs.endpoint:}") String endpoint) {
        SqsClientBuilder builder = SqsClient.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }
}
