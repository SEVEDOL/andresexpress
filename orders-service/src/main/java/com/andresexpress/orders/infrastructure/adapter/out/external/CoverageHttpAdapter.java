package com.andresexpress.orders.infrastructure.adapter.out.external;

import com.andresexpress.orders.application.port.out.CoverageServicePort;
import com.andresexpress.orders.domain.exception.CoverageUnavailableException;
import com.andresexpress.orders.domain.exception.InvalidCityException;
import com.andresexpress.orders.domain.model.TariffResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class CoverageHttpAdapter implements CoverageServicePort {

    private final RestClient coverageRestClient;

    @Override
    public TariffResult validateAndCalculateTariff(String originCity, String destinationCity, Double weight) {
        try {
            CoverageResponse response = coverageRestClient.post()
                    .uri("/api/v1/coverage/validate")
                    .body(new CoverageRequest(originCity, destinationCity, weight))
                    .retrieve()
                    .body(CoverageResponse.class);

            if (response == null || response.totalTariff() == null) {
                throw new CoverageUnavailableException("Coverage respondió sin tarifa");
            }
            return new TariffResult(response.totalTariff());

        } catch (HttpClientErrorException e) {
            // 4xx: la ciudad no existe o los datos son inválidos
            throw new InvalidCityException("La ciudad de origen o destino no existe");
        } catch (HttpServerErrorException | ResourceAccessException e) {
            // 5xx, timeout o Coverage apagado
            throw new CoverageUnavailableException("El servicio de cobertura no está disponible");
        }
    }
}
