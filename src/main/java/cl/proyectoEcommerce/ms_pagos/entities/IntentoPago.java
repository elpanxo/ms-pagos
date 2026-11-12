package cl.proyectoEcommerce.ms_pagos.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "intentos_pago", indexes = @Index(name = "idx_intentos_client_created", columnList = "client_id, created_at"))
public class IntentoPago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "client_id", nullable = false, length = 150)
    private String clientId;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    protected IntentoPago() {}
    public IntentoPago(String clientId, Long orderId, Instant createdAt) {
        this.clientId = clientId; this.orderId = orderId; this.createdAt = createdAt;
    }
}
