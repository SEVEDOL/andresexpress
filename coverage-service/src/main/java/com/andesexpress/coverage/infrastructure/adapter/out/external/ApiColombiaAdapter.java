package com.andesexpress.coverage.infrastructure.adapter.out.external;

import com.andesexpress.coverage.application.port.out.ApiColombiaPort;
import com.andesexpress.coverage.domain.exception.ExternalServiceUnavailableException;
import com.andesexpress.coverage.domain.model.CityData;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaCityResponseDTO;
import com.andesexpress.coverage.infrastructure.adapter.out.external.dto.ApiColombiaDepartmentResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.HttpStatusCodeException;
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
 * - Ante un 429 (demasiadas peticiones) o un 5xx se reintenta con espera.
 */
@Slf4j
@Component
public class ApiColombiaAdapter implements ApiColombiaPort {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final Pattern DISTRITO_CAPITAL = Pattern.compile(",?\\s*d\\.?\\s*c\\.?$");

    /** Tope de espera entre reintentos, aunque API Colombia pida mas con Retry-After. */
    private static final long MAX_RETRY_WAIT_MS = 5_000;
    /** Si la recarga falla y hay listas viejas, se vuelve a intentar despues de este tiempo, no en cada peticion. */
    private static final Duration REFRESH_RETRY_AFTER_FAILURE = Duration.ofMinutes(1);

    private final RestTemplate restTemplate;
    private final String citiesUrl;
    private final String departmentsUrl;
    private final Duration cacheTtl;
    private final int maxRetries;
    private final long retryBackoffMs;
    private final boolean warmUpOnStartup;

    private final Object loadLock = new Object();
    private volatile Catalog cachedCatalog;
    private volatile Instant refreshAfter = Instant.MIN;

    public ApiColombiaAdapter(RestTemplate restTemplate,
                              @Value("${api.colombia.url:https://api-colombia.com/api/v1/City}") String citiesUrl,
                              @Value("${api.colombia.departments-url:https://api-colombia.com/api/v1/Department}") String departmentsUrl,
                              @Value("${api.colombia.cache-minutes:60}") long cacheMinutes,
                              @Value("${api.colombia.max-retries:3}") int maxRetries,
                              @Value("${api.colombia.retry-backoff-ms:1000}") long retryBackoffMs,
                              @Value("${api.colombia.warm-up:true}") boolean warmUpOnStartup) {
        this.restTemplate = restTemplate;
        this.citiesUrl = citiesUrl;
        this.departmentsUrl = departmentsUrl;
        this.cacheTtl = Duration.ofMinutes(cacheMinutes);
        this.maxRetries = maxRetries;
        this.retryBackoffMs = retryBackoffMs;
        this.warmUpOnStartup = warmUpOnStartup;
    }

    /** Descarga las listas al arrancar para que la primera cotizacion no espere a API Colombia. */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!warmUpOnStartup) {
            return;
        }
        try {
            getCatalog();
        } catch (ExternalServiceUnavailableException e) {
            log.warn("No se pudieron precargar las listas de API Colombia; se intentara en la primera cotizacion");
        }
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

    /**
     * Devuelve las listas en cache. Si hay que recargarlas, solo un hilo llama a API Colombia;
     * las peticiones concurrentes esperan su resultado en vez de descargar cada una (RNF-02).
     */
    private Catalog getCatalog() {
        Catalog catalog = cachedCatalog;
        if (catalog != null && Instant.now().isBefore(refreshAfter)) {
            return catalog;
        }

        synchronized (loadLock) {
            catalog = cachedCatalog;
            if (catalog != null && Instant.now().isBefore(refreshAfter)) {
                return catalog; // otro hilo ya la recargo mientras se esperaba el lock
            }
            try {
                // Con listas viejas disponibles no se reintenta: se responde con ellas sin demorar al cliente.
                Catalog loaded = download(catalog == null ? maxRetries : 0);
                cachedCatalog = loaded;
                refreshAfter = Instant.now().plus(cacheTtl);
                return loaded;
            } catch (RestClientException e) {
                if (catalog != null) {
                    log.warn("API Colombia no respondio; se usan las listas en cache. Detalle: {}", e.getMessage());
                    refreshAfter = Instant.now().plus(REFRESH_RETRY_AFTER_FAILURE);
                    return catalog;
                }
                log.error("API Colombia no respondio y no hay cache: {}", e.getMessage());
                throw new ExternalServiceUnavailableException("API Colombia no esta disponible en este momento");
            }
        }
    }

    /** Descarga ambas listas; ante 429 o 5xx reintenta con espera creciente (RNF-08). */
    private Catalog download(int retries) {
        for (int attempt = 0; ; attempt++) {
            try {
                ApiColombiaCityResponseDTO[] cities = restTemplate.getForObject(citiesUrl, ApiColombiaCityResponseDTO[].class);
                ApiColombiaDepartmentResponseDTO[] departments =
                        restTemplate.getForObject(departmentsUrl, ApiColombiaDepartmentResponseDTO[].class);
                if (cities == null || departments == null) {
                    throw new ExternalServiceUnavailableException("API Colombia respondio sin datos");
                }
                log.info("Listas de API Colombia cargadas en cache: {} ciudades, {} departamentos",
                        cities.length, departments.length);
                return new Catalog(Arrays.asList(cities), Arrays.asList(departments));
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
                if (attempt >= retries) {
                    throw e;
                }
                long waitMs = retryWaitMs(e, attempt);
                log.warn("API Colombia respondio {}; reintento {}/{} en {} ms",
                        e.getStatusCode().value(), attempt + 1, retries, waitMs);
                sleep(waitMs);
            }
        }
    }

    /** Respeta Retry-After (en segundos) si API Colombia lo envia; si no, espera 1x, 2x, 4x el backoff. */
    private long retryWaitMs(HttpStatusCodeException e, int attempt) {
        long waitMs = retryBackoffMs << attempt;
        String retryAfter = e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Retry-After") : null;
        if (retryAfter != null) {
            try {
                waitMs = Long.parseLong(retryAfter.trim()) * 1000;
            } catch (NumberFormatException ignored) {
                // Retry-After tambien puede venir como fecha; en ese caso se usa el backoff
            }
        }
        return Math.min(waitMs, MAX_RETRY_WAIT_MS);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
