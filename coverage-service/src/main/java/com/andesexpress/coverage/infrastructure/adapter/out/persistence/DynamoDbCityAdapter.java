package com.andesexpress.coverage.infrastructure.adapter.out.persistence;

import com.andesexpress.coverage.application.port.out.CityCachePort;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.infrastructure.adapter.out.persistence.entity.CityCacheEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;

import java.text.Normalizer;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Repository
public class DynamoDbCityAdapter implements CityCachePort {

    private final DynamoDbTable<CityCacheEntity> table;

    public DynamoDbCityAdapter(DynamoDbEnhancedClient enhancedClient,
                               @Value("${aws.dynamodb.tableName:coverage_cities}") String tableName) {
        this.table = enhancedClient.table(tableName, TableSchema.fromBean(CityCacheEntity.class));
    }

    @Override
    public Optional<CityData> findByName(String cityName) {
        String normalized = normalize(cityName);
        Key key = Key.builder()
                .partitionValue("CITY#" + normalized)
                .sortValue("METADATA")
                .build();

        CityCacheEntity entity = table.getItem(key);
        if (entity == null) {
            return Optional.empty();
        }

        return Optional.of(new CityData(entity.getCityName(), entity.getDepartmentName()));
    }

    @Override
    public void save(CityData cityData) {
        String normalizedCity = normalize(cityData.getName());
        String normalizedDept = normalize(cityData.getDepartmentName());

        CityCacheEntity entity = new CityCacheEntity();
        entity.setPk("CITY#" + normalizedCity);
        entity.setSk("METADATA");
        entity.setGsiPk("DEPARTMENT#" + normalizedDept);
        entity.setGsiSk("CITY#" + normalizedCity);
        entity.setCityName(cityData.getName());
        entity.setDepartmentName(cityData.getDepartmentName());
        // TTL configurable (ej. 30 días de expiración)
        entity.setTtl(Instant.now().plusSeconds(30L * 24 * 3600).getEpochSecond());

        table.putItem(entity);
    }

    private String normalize(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("").toUpperCase().trim();
    }
}