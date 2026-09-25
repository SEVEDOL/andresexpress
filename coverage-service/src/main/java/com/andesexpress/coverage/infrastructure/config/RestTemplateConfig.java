package com.andesexpress.coverage.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    /**
     * Cliente HTTP hacia API Colombia con timeouts (RT-07: la red puede fallar o demorarse).
     * Sin timeout, si API Colombia se cuelga, Coverage (y Orders) se quedan esperando indefinidamente.
     */
    @Bean
    public RestTemplate restTemplate(@Value("${api.colombia.connect-timeout-ms:5000}") long connectTimeoutMs,
                                     @Value("${api.colombia.read-timeout-ms:15000}") long readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new RestTemplate(factory);
    }
}
