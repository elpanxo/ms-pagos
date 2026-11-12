package cl.proyectoEcommerce.ms_pagos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecurityConfig {
    private final String tenantId;
    private final String clientId;
    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${spring.security.oauth2.resourceserver.jwt.tenant-id}") String tenantId,
            @Value("${spring.security.oauth2.resourceserver.jwt.client-id}") String clientId,
            @Value("${cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.tenantId = tenantId;
        this.clientId = clientId;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isBlank()).toList();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/api/pagos/retorno").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/pagos/iniciar").hasAnyRole("Admin", "User")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.decoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        String jwkSetUri = "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> timestamp = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> issuer = jwt -> {
            String actual = jwt.getIssuer() == null ? "" : jwt.getIssuer().toString();
            if (actual.equals("https://login.microsoftonline.com/" + tenantId + "/v2.0")
                    || actual.equals("https://sts.windows.net/" + tenantId + "/")) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_issuer", "Emisor Azure AD no válido.", null));
        };
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> values = jwt.getAudience();
            if (values != null && (values.contains(clientId) || values.contains("api://" + clientId))) return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_audience", "La audiencia del token no corresponde a esta API.", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, issuer, audience));
        return decoder;
    }
}
