package cl.proyectoEcommerce.ms_pagos.controllers;

import cl.proyectoEcommerce.ms_pagos.services.PagoBloqueadoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.kafka.KafkaException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(PagoBloqueadoException.class)
    ResponseEntity<Map<String, Object>> bloqueado(PagoBloqueadoException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "message", ex.getMessage(), "blockedUntil", ex.getBloqueadoHasta().toString()));
    }
    @ExceptionHandler(SecurityException.class)
    ResponseEntity<Map<String, String>> prohibido(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> solicitudInvalida(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<Map<String, String>> conflicto(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", ex.getMessage()));
    }
    @ExceptionHandler(HttpClientErrorException.NotFound.class)
    ResponseEntity<Map<String, String>> ordenNoEncontrada(HttpClientErrorException.NotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "No se encontró la orden."));
    }
    @ExceptionHandler(HttpClientErrorException.class)
    ResponseEntity<Map<String, String>> errorOrden(HttpClientErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", "No se pudo validar la orden con ms-ordenes."));
    }
    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<Map<String, String>> msOrdenesNoDisponible(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("message", "ms-ordenes no está disponible."));
    }
    @ExceptionHandler(KafkaException.class)
    ResponseEntity<Map<String, String>> kafkaNoDisponible(KafkaException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("message", "No se pudo registrar el intento antifraude en Kafka. Intenta nuevamente."));
    }
}
