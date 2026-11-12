package cl.proyectoEcommerce.ms_pagos.services;

import java.time.Instant;

public class PagoBloqueadoException extends RuntimeException {
    private final Instant bloqueadoHasta;
    public PagoBloqueadoException(Instant bloqueadoHasta) {
        super("Demasiados intentos de pago. Espera 30 minutos antes de volver a intentarlo.");
        this.bloqueadoHasta = bloqueadoHasta;
    }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
}
