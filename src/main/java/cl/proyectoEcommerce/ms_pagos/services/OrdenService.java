package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.dtos.OrdenResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OrdenService {
    private final RestClient client;
    public OrdenService(RestClient ordenesRestClient) { this.client = ordenesRestClient; }

    public OrdenResponseDTO obtenerOrden(Long orderId, String bearerToken) {
        return client.get().uri("/{id}", orderId)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .retrieve().body(OrdenResponseDTO.class);
    }
}
