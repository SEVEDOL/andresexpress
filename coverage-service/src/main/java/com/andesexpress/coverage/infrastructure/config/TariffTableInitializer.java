package com.andesexpress.coverage.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.Map;

/**
 * Al arrancar: crea la tabla de tarifas si no existe y carga las tarifas por defecto
 * SOLO para las zonas que aun no tengan valor (si alguien las edita en la consola de AWS, se respetan).
 */
@Slf4j
@Component
public class TariffTableInitializer implements ApplicationRunner {

    // zona -> { baseTariff, includedKg, extraKgRate } (valores en COP y kg)
    private static final Map<String, String[]> DEFAULT_TARIFFS = Map.of(
            "SAME_CITY", new String[]{"10000", "2", "2000"},
            "SAME_DEPARTMENT", new String[]{"15000", "2", "2000"},
            "REST_OF_COUNTRY", new String[]{"25000", "2", "2000"});

    private final DynamoDbClient dynamoDb;
    private final String tableName;
    private final boolean createTables;

    public TariffTableInitializer(DynamoDbClient dynamoDb,
                                  @Value("${coverage.tariffs-table}") String tableName,
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
        createTableIfMissing();
        DEFAULT_TARIFFS.forEach(this::seedIfMissing);
    }

    private void createTableIfMissing() {
        try {
            dynamoDb.describeTable(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Tabla DynamoDB '{}' encontrada", tableName);
        } catch (ResourceNotFoundException notFound) {
            log.info("Creando tabla DynamoDB '{}'...", tableName);
            dynamoDb.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(KeySchemaElement.builder().attributeName("zone").keyType(KeyType.HASH).build())
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("zone").attributeType(ScalarAttributeType.S).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());
            dynamoDb.waiter().waitUntilTableExists(DescribeTableRequest.builder().tableName(tableName).build());
            log.info("Tabla DynamoDB '{}' creada", tableName);
        }
    }

    private void seedIfMissing(String zone, String[] values) {
        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(Map.of(
                            "zone", AttributeValue.fromS(zone),
                            "baseTariff", AttributeValue.fromN(values[0]),
                            "includedKg", AttributeValue.fromN(values[1]),
                            "extraKgRate", AttributeValue.fromN(values[2])))
                    .conditionExpression("attribute_not_exists(#z)")
                    .expressionAttributeNames(Map.of("#z", "zone"))
                    .build());
            log.info("Tarifa por defecto cargada para la zona {}", zone);
        } catch (ConditionalCheckFailedException alreadyExists) {
            // La zona ya tiene tarifa: no se sobrescribe.
        }
    }
}
