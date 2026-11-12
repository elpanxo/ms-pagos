package cl.proyectoEcommerce.ms_pagos.dtos;

import java.math.BigDecimal;

public record PagoResultadoDTO(Long orderId, String status, BigDecimal amount, String authorizationCode,
                               String message) {}
