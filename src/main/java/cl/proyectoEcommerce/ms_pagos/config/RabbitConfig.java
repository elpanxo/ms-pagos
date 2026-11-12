package cl.proyectoEcommerce.ms_pagos.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    TopicExchange pedidos360Exchange(@Value("${pedidos360.exchange:pedidos360.exchange}") String exchange) {
        return new TopicExchange(exchange, true, false);
    }
}
