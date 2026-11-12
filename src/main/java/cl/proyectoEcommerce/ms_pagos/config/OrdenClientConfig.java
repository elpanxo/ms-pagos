package cl.proyectoEcommerce.ms_pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OrdenClientConfig {
    @Bean
    RestClient ordenesRestClient(@Value("${services.ordenes.url:http://localhost:8084/api/v1/ordenes}") String url) {
        return RestClient.builder().baseUrl(url).build();
    }
}
