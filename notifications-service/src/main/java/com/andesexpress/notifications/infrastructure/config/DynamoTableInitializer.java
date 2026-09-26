package com.andesexpress.notifications.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/** Crea la tabla del log de notificaciones al arrancar, si no existe (se puede desactivar). */
@Slf4j
@Component
public class DynamoTableInitializer implements ApplicationRunner {

    private final DynamoDbClient dynamoDb;
    private final String tableName;
    private final boolean createTables;

    public DynamoTableInitializer(DynamoDbClient dynamoDb,
                                  @Value("${notifications.table-name}") String tableName,
                                  @Value("${aws.dynamodb.create-tables:true}") boolean createTables) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
        this.createTables = createTables;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!createTables) {
            return;
        }
        try {
            dynamoDb.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Tabla DynamoDB '{}' encontrada", tableName);
        } catch (ResourceNotFoundException notFound) {
            log.info("Creando tabla DynamoDB '{}'...", tableName);
            dynamoDb.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder().attributeName("eventId").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("eventId").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            dynamoDb.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Tabla DynamoDB '{}' creada", tableName);
        }
    }
}
