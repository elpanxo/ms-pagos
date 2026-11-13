package cl.proyectoEcommerce.ms_pagos.dtos;

import java.time.Instant;

public record IntentoPagoDTO(
        String clientId,
        Long amount,
        String transactionId,
        Instant timestamp,
        Long orderId) {
}
