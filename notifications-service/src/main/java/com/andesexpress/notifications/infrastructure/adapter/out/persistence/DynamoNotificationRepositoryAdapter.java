package com.andesexpress.notifications.infrastructure.adapter.out.persistence;

import com.andesexpress.notifications.application.port.out.NotificationRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Log de notificaciones en DynamoDB. Tabla con clave de particion "eventId" (String).
 *
 * La idempotencia usa una escritura CONDICIONAL: "guarda el item solo si ese eventId no existe".
 * DynamoDB lo evalua de forma atomica, asi que dos copias simultaneas del mismo evento
 * nunca pueden registrarse las dos.
 */
@Component
public class DynamoNotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public DynamoNotificationRepositoryAdapter(DynamoDbClient dynamoDb,
                                               @Value("${notifications.table-name}") String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    @Override
    public boolean tryRegister(String eventId, String recipientEmail) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("eventId", AttributeValue.fromS(eventId));
        item.put("status", AttributeValue.fromS("PROCESSING"));
        item.put("createdAt", AttributeValue.fromS(Instant.now().toString()));
        if (recipientEmail != null) {
            item.put("recipientEmail", AttributeValue.fromS(recipientEmail));
        }

        try {
            dynamoDb.putItem(PutItemRequest.builder()
                    .tableName(tableName)
                    .item(item)
                    .conditionExpression("attribute_not_exists(eventId)")
                    .build());
            return true;
        } catch (ConditionalCheckFailedException duplicate) {
            return false;
        }
    }

    @Override
    public void markAsSent(String eventId) {
        dynamoDb.updateItem(UpdateItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.fromS(eventId)))
                // "status" es palabra reservada en DynamoDB: se usa el alias #s
                .updateExpression("SET #s = :sent, sentAt = :now")
                .expressionAttributeNames(Map.of("#s", "status"))
                .expressionAttributeValues(Map.of(
                        ":sent", AttributeValue.fromS("SENT"),
                        ":now", AttributeValue.fromS(Instant.now().toString())))
                .build());
    }

    @Override
    public void release(String eventId) {
        dynamoDb.deleteItem(DeleteItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("eventId", AttributeValue.fromS(eventId)))
                .build());
    }
}
