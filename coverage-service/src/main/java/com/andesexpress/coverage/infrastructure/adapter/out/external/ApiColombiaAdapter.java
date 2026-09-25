package com.andesexpress.coverage.infrastructure.adapter.out.external;

import com.andesexpress.coverage.application.port.out.ApiColombiaPort;
import com.andesexpress.coverage.domain.exception.ExternalServiceUnavailableException;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaCityResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Adaptador de salida hacia API Colombia.
 *
 * - Ciudad que no existe  -> Optional.empty()  (el servicio responde 404)
 * - API Colombia caida    -> ExternalServiceUnavailableException (el servicio responde 503)
 * - La lista de ciudades se guarda en memoria (cache) para no descargarla en cada pedido.
 *   Si API Colombia falla y ya hay una lista guardada, se usa esa.
 */
@Slf4j
@Component
public class ApiColombiaAdapter implements ApiColombiaPort {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final Duration cacheTtl;

    private volatile List<ApiColombiaCityResponseDTO> cachedCities;
    private volatile Instant cacheLoadedAt;

    public ApiColombiaAdapter(RestTemplate restTemplate,
                              @Value("${api.colombia.url:https://api-colombia.com/api/v1/City}") String apiUrl,
                              @Value("${api.colombia.cache-minutes:60}") long cacheMinutes) {
        this.restTemplate = restTemplate;
        this.apiUrl = apiUrl;
        this.cacheTtl = Duration.ofMinutes(cacheMinutes);
    }

    @Override
    public Optional<CityData> fetchCityByName(String cityName) {
        Optional<ApiColombiaCityResponseDTO> match = getCities().stream()
                .filter(dto -> matchesCityName(dto.getName(), cityName))
                .findFirst();

        if (match.isEmpty()) {
            return Optional.empty();
        }

        ApiColombiaCityResponseDTO detail;
        try {
            detail = restTemplate.getForObject(apiUrl + "/" + match.get().getId(), ApiColombiaCityResponseDTO.class);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("API Colombia no respondio al consultar la ciudad {}: {}", cityName, e.getMessage());
            throw new ExternalServiceUnavailableException("API Colombia no esta disponible en este momento");
        }

        if (detail == null) {
            return Optional.empty();
        }

        return Optional.of(new CityData(
                detail.getName(),
                detail.getDepartment() != null ? detail.getDepartment().getName() : "Desconocido"
        ));
    }

    private List<ApiColombiaCityResponseDTO> getCities() {
        List<ApiColombiaCityResponseDTO> cities = cachedCities;
        Instant loadedAt = cacheLoadedAt;
        if (cities != null && loadedAt != null && loadedAt.plus(cacheTtl).isAfter(Instant.now())) {
            return cities;
        }

        try {
            ApiColombiaCityResponseDTO[] response = restTemplate.getForObject(apiUrl, ApiColombiaCityResponseDTO[].class);
            if (response == null) {
                throw new ExternalServiceUnavailableException("API Colombia respondio sin datos");
            }
            cities = Arrays.asList(response);
            cachedCities = cities;
            cacheLoadedAt = Instant.now();
            log.info("Lista de ciudades de API Colombia cargada en cache: {} ciudades", cities.size());
            return cities;
        } catch (RestClientException e) {
            if (cachedCities != null) {
                log.warn("API Colombia no respondio; se usa la lista de ciudades en cache. Detalle: {}", e.getMessage());
                return cachedCities;
            }
            log.error("API Colombia no respondio y no hay cache: {}", e.getMessage());
            throw new ExternalServiceUnavailableException("API Colombia no esta disponible en este momento");
        }
    }

    private boolean matchesCityName(String apiName, String requestedName) {
        String normalizedApiName = normalizeCityName(apiName).trim();
        String normalizedRequestedName = normalizeCityName(requestedName).trim();
        return normalizedApiName.equalsIgnoreCase(normalizedRequestedName)
                || normalizedApiName.toLowerCase().startsWith(normalizedRequestedName.toLowerCase() + " ");
    }

    private String normalizeCityName(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return DIACRITICS.matcher(normalized).replaceAll("");
    }
}
