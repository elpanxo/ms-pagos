package cl.proyectoEcommerce.ms_pagos.repositories;

import cl.proyectoEcommerce.ms_pagos.entities.IntentoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface IntentoPagoRepository extends JpaRepository<IntentoPago, Long> {
    boolean existsByTransactionId(String transactionId);
    long countByClientIdAndCreatedAtAfterAndCreatedAtLessThanEqual(String clientId, Instant from, Instant to);
    long deleteByClientIdAndCreatedAtBefore(String clientId, Instant before);
    long deleteByCreatedAtBefore(Instant before);
}
