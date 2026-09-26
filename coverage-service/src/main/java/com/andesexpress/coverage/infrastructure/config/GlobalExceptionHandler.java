package com.andesexpress.coverage.infrastructure.config;

import com.andesexpress.coverage.domain.exception.CityNotFoundException;
import com.andesexpress.coverage.domain.exception.ExternalServiceUnavailableException;
import com.andesexpress.coverage.domain.exception.TariffNotConfiguredException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": valor requerido o invalido")
                .orElse("Solicitud invalida");
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCityNotFound(CityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleExternalUnavailable(ExternalServiceUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(TariffNotConfiguredException.class)
    public ResponseEntity<Map<String, String>> handleTariffNotConfigured(TariffNotConfiguredException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", ex.getMessage()));
    }
}
