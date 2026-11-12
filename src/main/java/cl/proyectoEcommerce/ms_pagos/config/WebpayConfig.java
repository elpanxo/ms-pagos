package cl.proyectoEcommerce.ms_pagos.config;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebpayConfig {
    @Bean
    WebpayPlus.Transaction webpayTransaction(
            @Value("${transbank.commerce-code:597055555532}") String commerceCode,
            @Value("${transbank.api-key:}") String apiKey,
            @Value("${transbank.environment:integration}") String environment) {
        if ("production".equalsIgnoreCase(environment)) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException("TRANSBANK_API_KEY es obligatorio para producción.");
            }
            return WebpayPlus.Transaction.buildForProduction(commerceCode, apiKey);
        }
        if (apiKey == null || apiKey.isBlank()) {
            return WebpayPlus.Transaction.buildForIntegration(commerceCode, IntegrationApiKeys.WEBPAY);
        }
        return WebpayPlus.Transaction.buildForIntegration(commerceCode, apiKey);
    }
}
