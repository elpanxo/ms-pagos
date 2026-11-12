package cl.proyectoEcommerce.ms_pagos.dtos;

public record IniciarPagoResponse(Long orderId, String url, String token) {}
