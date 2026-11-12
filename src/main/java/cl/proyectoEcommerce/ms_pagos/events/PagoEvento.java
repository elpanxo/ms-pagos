package cl.proyectoEcommerce.ms_pagos.events;

import java.math.BigDecimal;
import java.time.Instant;

public record PagoEvento(Long orderId, String clientId, String status, BigDecimal amount, Instant occurredAt) {}
