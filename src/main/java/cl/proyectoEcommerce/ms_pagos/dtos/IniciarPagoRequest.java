package cl.proyectoEcommerce.ms_pagos.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record IniciarPagoRequest(@NotNull @Positive Long orderId) {}
