package cl.proyectoEcommerce.ms_pagos.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "estado_riesgo_pago")
public class EstadoRiesgoCliente {
    @Id
    @Column(name = "client_id", length = 150)
    private String clientId;

    @Column(nullable = false)
    private boolean blocked;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "reset_at", nullable = false)
    private Instant resetAt;

    protected EstadoRiesgoCliente() {}

    public EstadoRiesgoCliente(String clientId, Instant now) {
        this.clientId = clientId;
        this.updatedAt = now;
        this.resetAt = now;
    }

    public String getClientId() { return clientId; }
    public boolean isBlocked() { return blocked; }
    public int getAttemptCount() { return attemptCount; }
    public Instant getBlockedUntil() { return blockedUntil; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResetAt() { return resetAt; }

    public void registerAttempts(int attemptCount, Instant now) {
        this.attemptCount = attemptCount;
        this.updatedAt = now;
    }

    public void blockUntil(Instant until, Instant now) {
        this.blocked = true;
        this.blockedUntil = until;
        this.updatedAt = now;
    }

    public void reset(Instant now) {
        this.blocked = false;
        this.attemptCount = 0;
        this.blockedUntil = null;
        this.updatedAt = now;
        this.resetAt = now;
    }
}
