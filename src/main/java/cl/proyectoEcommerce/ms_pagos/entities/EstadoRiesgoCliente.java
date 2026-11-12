package cl.proyectoEcommerce.ms_pagos.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "estado_riesgo_pago")
public class EstadoRiesgoCliente {
    @Id @Column(name = "client_id", length = 150)
    private String clientId;
    @Column(name = "blocked_until")
    private Instant blockedUntil;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    protected EstadoRiesgoCliente() {}
    public EstadoRiesgoCliente(String clientId) { this.clientId = clientId; this.updatedAt = Instant.now(); }
    public String getClientId() { return clientId; }
    public Instant getBlockedUntil() { return blockedUntil; }
    public void setBlockedUntil(Instant blockedUntil) { this.blockedUntil = blockedUntil; this.updatedAt = Instant.now(); }
}
