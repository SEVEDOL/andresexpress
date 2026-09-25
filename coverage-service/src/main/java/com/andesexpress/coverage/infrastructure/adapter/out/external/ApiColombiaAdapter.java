package com.andesexpress.coverage.infrastructure.adapter.out.external;

import com.andesexpress.coverage.application.port.out.ApiColombiaPort;
import com.andesexpress.coverage.application.port.out.CityCachePort;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaCityResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ApiColombiaAdapter implements ApiColombiaPort {

    private final RestTemplate restTemplate;
    private final CityCachePort cityCachePort;

    @Value("${api.colombia.url:https://api-colombia.com/api/v1/City}")
    private String apiUrl;

    @Override
    public Optional<CityData> fetchCityByName(String cityName) {
        // 1. Intentar obtener desde DynamoDB (Capa Local / Caché)
        Optional<CityData> cachedCity = cityCachePort.findByName(cityName);
        if (cachedCity.isPresent()) {
            return cachedCity;
        }

        // 2. Si no está en caché, consultar API Colombia (Fuente Oficial)
        try {
            ApiColombiaCityResponseDTO[] response = restTemplate.getForObject(
                    apiUrl, ApiColombiaCityResponseDTO[].class);

            if (response != null) {
                Optional<ApiColombiaCityResponseDTO> cityMatch = Arrays.stream(response)
                        .filter(dto -> matchesCityName(dto.getName(), cityName))
                        .findFirst();

                if (cityMatch.isEmpty()) {
                    return Optional.empty();
                }

                ApiColombiaCityResponseDTO dto = restTemplate.getForObject(
                        apiUrl + "/" + cityMatch.get().getId(), ApiColombiaCityResponseDTO.class);

                if (dto == null) {
                    return Optional.empty();
                }

                CityData cityData = new CityData(
                        dto.getName(),
                        dto.getDepartment() != null ? dto.getDepartment().getName() : "Desconocido"
                );

                // 3. Guardar en DynamoDB para futuras consultas
                cityCachePort.save(cityData);

                return Optional.of(cityData);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private boolean matchesCityName(String apiName, String requestedName) {
        String normalizedApiName = normalizeCityName(apiName).trim();
        String normalizedRequestedName = normalizeCityName(requestedName).trim();
        return normalizedApiName.equalsIgnoreCase(normalizedRequestedName)
                || normalizedApiName.startsWith(normalizedRequestedName + " ");
    }

    private String normalizeCityName(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalized).replaceAll("");
    }
}