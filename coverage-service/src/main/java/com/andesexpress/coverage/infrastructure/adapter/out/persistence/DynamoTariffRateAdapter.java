package com.andesexpress.coverage.infrastructure.adapter.out.persistence;

import com.andesexpress.coverage.application.port.out.TariffRatePort;
import com.andesexpress.coverage.domain.model.Zone;
import com.andesexpress.coverage.domain.model.ZoneTariff;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * Lee las tarifas por zona desde DynamoDB.
 * Tabla con clave de particion "zone" (String) y atributos numericos:
 * baseTariff, includedKg, extraKgRate.
 */
@Component
public class DynamoTariffRateAdapter implements TariffRatePort {

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public DynamoTariffRateAdapter(DynamoDbClient dynamoDb,
                                   @Value("${coverage.tariffs-table}") String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    @Override
    public Optional<ZoneTariff> findByZone(Zone zone) {
        GetItemResponse response = dynamoDb.getItem(GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of("zone", AttributeValue.fromS(zone.name())))
                .consistentRead(true)
                .build());

        if (!response.hasItem() || response.item().isEmpty()) {
            return Optional.empty();
        }
        Map<String, AttributeValue> item = response.item();
        return Optional.of(new ZoneTariff(
                zone,
                number(item, "baseTariff"),
                number(item, "includedKg"),
                number(item, "extraKgRate")));
    }

    private BigDecimal number(Map<String, AttributeValue> item, String attribute) {
        AttributeValue value = item.get(attribute);
        return value == null || value.n() == null ? BigDecimal.ZERO : new BigDecimal(value.n());
    }
}
