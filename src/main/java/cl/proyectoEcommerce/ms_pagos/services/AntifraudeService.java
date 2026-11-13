package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.dtos.IntentoPagoDTO;
import cl.proyectoEcommerce.ms_pagos.entities.EstadoRiesgoCliente;
import cl.proyectoEcommerce.ms_pagos.entities.IntentoPago;
import cl.proyectoEcommerce.ms_pagos.repositories.EstadoRiesgoRepository;
import cl.proyectoEcommerce.ms_pagos.repositories.IntentoPagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AntifraudeService {
    private static final Logger log = LoggerFactory.getLogger(AntifraudeService.class);
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

    /** Synchronous gate checked before publishing an event or creating a Webpay transaction. */
    @Transactional
    public Instant bloqueoActivoHasta(String clientId) {
        EstadoRiesgoCliente risk = riskRepository.findById(clientId).orElse(null);
        if (risk == null || !risk.isBlocked()) return null;
        Instant now = Instant.now();
        if (risk.getBlockedUntil() != null && risk.getBlockedUntil().isAfter(now)) return risk.getBlockedUntil();
        risk.reset(now);
        attemptRepository.deleteByClientIdAndCreatedAtBefore(clientId, now);
        log.info("Bloqueo antifraude expirado para cliente {}", clientId);
        return null;
    }

    /** Kafka consumer counts the event and applies the velocity rule under a per-client row lock. */
    @Transactional
    public void procesarIntento(IntentoPagoDTO event) {
        if (event.timestamp() == null || event.clientId() == null || event.transactionId() == null) {
            throw new IllegalArgumentException("El evento Kafka de intento de pago está incompleto.");
        }
        riskRepository.createIfMissing(event.clientId(), event.timestamp());
        EstadoRiesgoCliente risk = riskRepository.lockByClientId(event.clientId())
                .orElseThrow(() -> new IllegalStateException("No se pudo inicializar el estado antifraude"));

        if (event.timestamp().isBefore(risk.getResetAt())) return;
        if (attemptRepository.existsByTransactionId(event.transactionId())) return;

        attemptRepository.save(new IntentoPago(event.clientId(), event.orderId(), event.amount(),
                event.transactionId(), event.timestamp()));
        attemptRepository.flush();

        Instant from = event.timestamp().minus(windowSeconds, ChronoUnit.SECONDS);
        long attempts = attemptRepository.countByClientIdAndCreatedAtAfterAndCreatedAtLessThanEqual(
                event.clientId(), from, event.timestamp());
        Instant now = Instant.now();
        risk.registerAttempts((int) attempts, now);

        if (attempts >= maxAttempts) {
            Instant blockedUntil = now.plus(blockMinutes, ChronoUnit.MINUTES);
            risk.blockUntil(blockedUntil, now);
            log.warn("Velocity check bloqueó al cliente {} tras {} intentos en {} segundos. Hasta {}",
                    event.clientId(), attempts, windowSeconds, blockedUntil);
        }
    }

    /** Successful, rejected or cancelled Webpay callbacks clear prior attempts, as specified. */
    @Transactional
    public void reiniciarDespuesDeCallback(String clientId) {
        Instant now = Instant.now();
        riskRepository.findById(clientId).ifPresent(risk -> risk.reset(now));
        attemptRepository.deleteByClientIdAndCreatedAtBefore(clientId, now);
    }

    @Scheduled(fixedDelayString = "${fraud.cleanup-delay-ms:3600000}")
    @Transactional
    public void limpiarIntentosAntiguos() {
        attemptRepository.deleteByCreatedAtBefore(Instant.now().minus(1, ChronoUnit.DAYS));
    }
}
