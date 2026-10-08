package com.andesexpress.coverage.infrastructure.adapter.out.external;

import com.andesexpress.coverage.application.port.out.ApiColombiaPort;
import com.andesexpress.coverage.domain.exception.ExternalServiceUnavailableException;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaCityResponseDTO;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaDepartmentResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Adaptador de salida hacia API Colombia.
 *
 * - Ciudad que no existe en ese departamento -> Optional.empty()  (el servicio responde 404)
 * - API Colombia caida                       -> ExternalServiceUnavailableException (el servicio responde 503)
 * - Las listas de ciudades y departamentos se guardan en memoria (cache), asi que validar
 *   un pedido no llama a API Colombia. Si API Colombia falla y ya hay listas guardadas, se usan esas.
 */
@Slf4j
@Component
public class ApiColombiaAdapter implements ApiColombiaPort {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern DISTRITO_CAPITAL = Pattern.compile(",?\\s*d\\.?\\s*c\\.?$");

    private final RestTemplate restTemplate;
    private final String citiesUrl;
    private final String departmentsUrl;
    private final Duration cacheTtl;

    private volatile Catalog cachedCatalog;
    private volatile Instant cacheLoadedAt;

    public ApiColombiaAdapter(RestTemplate restTemplate,
                              @Value("${api.colombia.url:https://api-colombia.com/api/v1/City}") String citiesUrl,
                              @Value("${api.colombia.departments-url:https://api-colombia.com/api/v1/Department}") String departmentsUrl,
                              @Value("${api.colombia.cache-minutes:60}") long cacheMinutes) {
        this.restTemplate = restTemplate;
        this.citiesUrl = citiesUrl;
        this.departmentsUrl = departmentsUrl;
        this.cacheTtl = Duration.ofMinutes(cacheMinutes);
    }

    @Override
    public Optional<CityData> fetchCity(String cityName, String departmentName) {
        Catalog catalog = getCatalog();

        Optional<ApiColombiaDepartmentResponseDTO> department = catalog.departments().stream()
                .filter(dto -> matchesName(dto.getName(), departmentName))
                .findFirst();
        if (department.isEmpty()) {
            return Optional.empty();
        }

        Integer departmentId = department.get().getId();
        return catalog.cities().stream()
                .filter(dto -> departmentId.equals(dto.getDepartmentId()))
                .filter(dto -> matchesName(dto.getName(), cityName))
                .findFirst()
                .map(city -> new CityData(city.getName(), department.get().getName()));
    }

    private Catalog getCatalog() {
        Catalog catalog = cachedCatalog;
        Instant loadedAt = cacheLoadedAt;
        if (catalog != null && loadedAt != null && loadedAt.plus(cacheTtl).isAfter(Instant.now())) {
            return catalog;
        }

        try {
            ApiColombiaCityResponseDTO[] cities = restTemplate.getForObject(citiesUrl, ApiColombiaCityResponseDTO[].class);
            ApiColombiaDepartmentResponseDTO[] departments =
                    restTemplate.getForObject(departmentsUrl, ApiColombiaDepartmentResponseDTO[].class);
            if (cities == null || departments == null) {
                throw new ExternalServiceUnavailableException("API Colombia respondio sin datos");
            }
            catalog = new Catalog(Arrays.asList(cities), Arrays.asList(departments));
            cachedCatalog = catalog;
            cacheLoadedAt = Instant.now();
            log.info("Listas de API Colombia cargadas en cache: {} ciudades, {} departamentos",
                    cities.length, departments.length);
            return catalog;
        } catch (RestClientException e) {
            if (cachedCatalog != null) {
                log.warn("API Colombia no respondio; se usan las listas en cache. Detalle: {}", e.getMessage());
                return cachedCatalog;
            }
            log.error("API Colombia no respondio y no hay cache: {}", e.getMessage());
            throw new ExternalServiceUnavailableException("API Colombia no esta disponible en este momento");
        }
    }

    /**
     * Compara el nombre completo (RF-03, RN-05): un nombre parcial como "San" no debe
     * coincidir con "San Francisco".
     */
    static boolean matchesName(String apiName, String requestedName) {
        String normalizedRequestedName = normalizeName(requestedName);
        return !normalizedRequestedName.isEmpty()
                && normalizeName(apiName).equals(normalizedRequestedName);
    }

    /**
     * Sin tildes, en minusculas y con espacios simples. Quita el sufijo "D.C." para que
     * "Bogota" coincida con "Bogotá D.C.", el unico nombre de API Colombia que lo trae.
     */
    static String normalizeName(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        normalized = DIACRITICS.matcher(normalized).replaceAll("")
                .toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
        return DISTRITO_CAPITAL.matcher(normalized).replaceAll("");
    }

    private record Catalog(List<ApiColombiaCityResponseDTO> cities,
                           List<ApiColombiaDepartmentResponseDTO> departments) {
    }
}
