package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.dtos.IntentoPagoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IntentoPagoConsumer {
    private static final Logger log = LoggerFactory.getLogger(IntentoPagoConsumer.class);
    private final AntifraudeService antifraudeService;

    public IntentoPagoConsumer(AntifraudeService antifraudeService) {
        this.antifraudeService = antifraudeService;
    }

    @KafkaListener(topics = "${pagos.kafka.topic:payment-attempts}", groupId = "${spring.kafka.consumer.group-id:payment-velocity-group}")
    public void consumir(IntentoPagoDTO event) {
        log.info("Evento de intento recibido desde Kafka: clientId={}, transactionId={}",
                event.clientId(), event.transactionId());
        antifraudeService.procesarIntento(event);
    }
}
