package cl.proyectoEcommerce.ms_pagos.dtos;

import java.math.BigDecimal;

public record OrdenResponseDTO(Long id, String usuarioOid, BigDecimal total, String estado) {}
