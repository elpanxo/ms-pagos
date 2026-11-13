package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.dtos.IntentoPagoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaAttemptPublisher {
    private static final Logger log = LoggerFactory.getLogger(KafkaAttemptPublisher.class);
    private final KafkaTemplate<String, IntentoPagoDTO> kafkaTemplate;
    private final String topic;

    public KafkaAttemptPublisher(KafkaTemplate<String, IntentoPagoDTO> kafkaTemplate,
            @Value("${pagos.kafka.topic:payment-attempts}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicar(IntentoPagoDTO event) {
        try {
            var result = kafkaTemplate.send(topic, event.clientId(), event).get(10, TimeUnit.SECONDS);
            log.info("Intento {} enviado a Kafka: topic={}, partition={}, offset={}",
                    event.transactionId(), result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KafkaException("Se interrumpió la publicación del intento antifraude.", e);
        } catch (Exception e) {
            log.error("No se pudo publicar el intento de pago {} en Kafka", event.transactionId(), e);
            throw new KafkaException("Kafka no confirmó el registro del intento de pago.", e);
        }
    }
}
