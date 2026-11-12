package cl.proyectoEcommerce.ms_pagos.repositories;

import cl.proyectoEcommerce.ms_pagos.entities.IntentoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface IntentoPagoRepository extends JpaRepository<IntentoPago, Long> {
    long countByClientIdAndCreatedAtAfter(String clientId, Instant from);
}
