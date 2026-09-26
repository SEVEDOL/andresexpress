package com.andesexpress.notifications.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.net.URI;

/**
 * Cliente de DynamoDB.
 * - En AWS (EC2): sin endpoint; las credenciales salen solas del rol de la instancia (LabInstanceProfile).
 * - En local: DYNAMODB_ENDPOINT=http://localhost:8000 (DynamoDB Local en Docker) con credenciales falsas.
 */
@Configuration
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${aws.region}") String region,
                                         @Value("${aws.dynamodb.endpoint:}") String endpoint) {
        DynamoDbClientBuilder builder = DynamoDbClient.builder().region(Region.of(region));
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }
}
