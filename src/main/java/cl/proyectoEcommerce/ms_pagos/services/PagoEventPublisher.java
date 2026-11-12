package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.events.PagoEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;

@Component
public class PagoEventPublisher {
    private static final Logger log = LoggerFactory.getLogger(PagoEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    public PagoEventPublisher(RabbitTemplate rabbitTemplate,
            @Value("${pedidos360.exchange:pedidos360.exchange}") String exchange) {
        this.rabbitTemplate = rabbitTemplate; this.exchange = exchange;
    }
    public void publicar(Long orderId, String clientId, String status, BigDecimal amount) {
        try {
            rabbitTemplate.convertAndSend(exchange, "pedidos360.pago." + status.toLowerCase(),
                    new PagoEvento(orderId, clientId, status, amount, Instant.now()));
        } catch (Exception e) {
            log.warn("No se pudo publicar el evento de pago de la orden {}: {}", orderId, e.getMessage());
        }
    }
}
