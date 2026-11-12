package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.entities.EstadoRiesgoCliente;
import cl.proyectoEcommerce.ms_pagos.entities.IntentoPago;
import cl.proyectoEcommerce.ms_pagos.repositories.EstadoRiesgoRepository;
import cl.proyectoEcommerce.ms_pagos.repositories.IntentoPagoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AntifraudeService {
    private final EstadoRiesgoRepository riskRepository;
    private final IntentoPagoRepository attemptRepository;
    private final int maxAttempts;
    private final int windowSeconds;
    private final int blockMinutes;

    public AntifraudeService(EstadoRiesgoRepository riskRepository, IntentoPagoRepository attemptRepository,
            @Value("${fraud.max-attempts:3}") int maxAttempts,
            @Value("${fraud.window-seconds:10}") int windowSeconds,
            @Value("${fraud.block-minutes:30}") int blockMinutes) {
        this.riskRepository = riskRepository;
        this.attemptRepository = attemptRepository;
        this.maxAttempts = maxAttempts;
        this.windowSeconds = windowSeconds;
        this.blockMinutes = blockMinutes;
    }

    /** Returns the active or newly-created block expiry; null means the attempt may continue. */
    @Transactional
    public Instant registrarIntento(String clientId, Long orderId) {
        riskRepository.createIfMissing(clientId);
        EstadoRiesgoCliente risk = riskRepository.lockByClientId(clientId)
                .orElseThrow(() -> new IllegalStateException("No se pudo inicializar el estado antifraude"));
        Instant now = Instant.now();
        if (risk.getBlockedUntil() != null && risk.getBlockedUntil().isAfter(now)) {
            return risk.getBlockedUntil();
        }
        if (risk.getBlockedUntil() != null) risk.setBlockedUntil(null);

        attemptRepository.save(new IntentoPago(clientId, orderId, now));
        long recentAttempts = attemptRepository.countByClientIdAndCreatedAtAfter(clientId, now.minus(windowSeconds, ChronoUnit.SECONDS));
        if (recentAttempts >= maxAttempts) {
            Instant blockedUntil = now.plus(blockMinutes, ChronoUnit.MINUTES);
            risk.setBlockedUntil(blockedUntil);
            return blockedUntil;
        }
        return null;
    }

    @Transactional
    public void limpiarBloqueo(String clientId) {
        riskRepository.findById(clientId).ifPresent(risk -> risk.setBlockedUntil(null));
    }
}
