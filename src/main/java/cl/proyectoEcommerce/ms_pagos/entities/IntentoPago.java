package cl.proyectoEcommerce.ms_pagos.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "intentos_pago", uniqueConstraints = {
        @UniqueConstraint(name = "uk_intentos_transaction_id", columnNames = "transaction_id")
}, indexes = @Index(name = "idx_intentos_client_created", columnList = "client_id, created_at"))
public class IntentoPago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "client_id", nullable = false, length = 150)
    private String clientId;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "transaction_id", nullable = false, length = 80)
    private String transactionId;
    @Column(nullable = false, precision = 12, scale = 0)
    private Long amount;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IntentoPago() {}
    public IntentoPago(String clientId, Long orderId, Long amount, String transactionId, Instant createdAt) {
        this.clientId = clientId;
        this.orderId = orderId;
        this.amount = amount;
        this.transactionId = transactionId;
        this.createdAt = createdAt;
    }
}
