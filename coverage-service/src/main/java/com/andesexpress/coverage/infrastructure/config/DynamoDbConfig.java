package com.andesexpress.coverage.infrastructure.config;

import com.andesexpress.coverage.infrastructure.adapter.out.persistence.entity.CityCacheEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.ProjectionType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.dynamodb.endpoint:http://localhost:8000}")
    private String endpoint;

    @Value("${aws.credentials.access-key:fakeMyKeyId}")
    private String accessKey;

    @Value("${aws.credentials.secret-key:fakeSecretAccessKey}")
    private String secretKey;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)
                ))
                .build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    @Bean
    public ApplicationRunner createTableRunner(DynamoDbEnhancedClient enhancedClient,
                                               @Value("${aws.dynamodb.tableName:coverage_cities}") String tableName) {
        return args -> {
            DynamoDbTable<CityCacheEntity> table = enhancedClient.table(tableName, TableSchema.fromBean(CityCacheEntity.class));
            try {
                table.createTable(builder -> builder
                        .provisionedThroughput(b -> b.readCapacityUnits(5L).writeCapacityUnits(5L))
                        .globalSecondaryIndices(gsi -> gsi
                                .indexName("GSI_Department")
                                .provisionedThroughput(b -> b.readCapacityUnits(5L).writeCapacityUnits(5L))
                                .projection(p -> p.projectionType(ProjectionType.ALL))
                        )
                );
                System.out.println(">>> [DynamoDB Local] Tabla '" + tableName + "' y GSI 'GSI_Department' creados correctamente.");
            } catch (ResourceInUseException e) {
                System.out.println(">>> [DynamoDB Local] La tabla '" + tableName + "' ya existe. Listo para operar.");
            } catch (Exception e) {
                System.err.println(">>> [DynamoDB Local] Advertencia al crear la tabla '" + tableName + "': " + e.getMessage());
            }
        };
    }
}