package cl.proyectoEcommerce.ms_pagos.services;

import cl.proyectoEcommerce.ms_pagos.dtos.IniciarPagoResponse;
import cl.proyectoEcommerce.ms_pagos.dtos.IntentoPagoDTO;
import cl.proyectoEcommerce.ms_pagos.dtos.OrdenResponseDTO;
import cl.proyectoEcommerce.ms_pagos.dtos.PagoResultadoDTO;
import cl.proyectoEcommerce.ms_pagos.entities.Pago;
import cl.proyectoEcommerce.ms_pagos.repositories.PagoRepository;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Service
public class PagoService {
    private static final Logger log = LoggerFactory.getLogger(PagoService.class);
    private final PagoRepository pagoRepository;
    private final OrdenService ordenService;
    private final AntifraudeService antifraudeService;
    private final PagoEventPublisher eventPublisher;
    private final KafkaAttemptPublisher kafkaAttemptPublisher;
    private final WebpayPlus.Transaction webpay;
    private final String frontendUrl;
    private final String callbackUrl;

    public PagoService(PagoRepository pagoRepository, OrdenService ordenService, AntifraudeService antifraudeService,
            PagoEventPublisher eventPublisher, KafkaAttemptPublisher kafkaAttemptPublisher,
            WebpayPlus.Transaction webpay,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
            @Value("${transbank.return-url:http://localhost:8083/api/pagos/retorno}") String callbackUrl) {
        this.pagoRepository = pagoRepository; this.ordenService = ordenService;
        this.antifraudeService = antifraudeService; this.eventPublisher = eventPublisher;
        this.kafkaAttemptPublisher = kafkaAttemptPublisher; this.webpay = webpay; this.frontendUrl = frontendUrl.replaceAll("/$", ""); this.callbackUrl = callbackUrl;
    }

    public IniciarPagoResponse iniciar(Long orderId, String clientId, String bearerToken) {
        OrdenResponseDTO order = ordenService.obtenerOrden(orderId, bearerToken);
        if (order == null || !orderId.equals(order.id())) throw new IllegalArgumentException("No se encontró la orden.");
        if (!clientId.equals(order.usuarioOid())) throw new SecurityException("La orden no pertenece al usuario autenticado.");
        if (order.total() == null || order.total().signum() <= 0) throw new IllegalArgumentException("El total de la orden no es válido.");
        BigDecimal amount;
        try { amount = order.total().setScale(0, RoundingMode.UNNECESSARY); }
        catch (ArithmeticException e) { throw new IllegalArgumentException("Webpay requiere un monto entero en pesos chilenos."); }

        Pago existing = pagoRepository.findByOrderId(orderId).orElse(null);
        if (existing != null && "PENDING".equals(existing.getStatus()) && existing.getWebpayToken() != null) {
            return new IniciarPagoResponse(orderId, existing.getWebpayUrl(), existing.getWebpayToken());
        }
        if (existing != null && !java.util.Set.of("ERROR", "REJECTED", "CANCELLED").contains(existing.getStatus())) {
            throw new IllegalStateException("Esta orden ya tiene un pago registrado: " + existing.getStatus());
        }

        Instant blockedUntil = antifraudeService.bloqueoActivoHasta(clientId);
        if (blockedUntil != null) {
            eventPublisher.publicar(orderId, clientId, "bloqueado", amount);
            throw new PagoBloqueadoException(blockedUntil);
        }

        String attemptId = "TX-" + UUID.randomUUID();
        kafkaAttemptPublisher.publicar(new IntentoPagoDTO(clientId, amount.longValueExact(), attemptId,
                Instant.now(), orderId));

        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String buyOrder = "O" + Long.toString(orderId, 36) + "-" + suffix;
        String sessionId = UUID.randomUUID().toString();
        Pago pago;
        if (existing == null) pago = new Pago(orderId, clientId, buyOrder, sessionId, amount);
        else { existing.reiniciarTransaccion(buyOrder, sessionId, amount); pago = existing; }
        pago = pagoRepository.save(pago);
        try {
            var response = webpay.create(buyOrder, sessionId, amount.doubleValue(), callbackUrl);
            pago.setWebpayToken(response.getToken());
            pago.setWebpayUrl(response.getUrl());
            pago.setStatus("PENDING");
            pagoRepository.save(pago);
            eventPublisher.publicar(orderId, clientId, "iniciado", amount);
            return new IniciarPagoResponse(orderId, response.getUrl(), response.getToken());
        } catch (Exception e) {
            pago.setStatus("ERROR");
            pagoRepository.save(pago);
            log.error("Error creando transacción Webpay para la orden {}", orderId, e);
            throw new IllegalStateException("No se pudo iniciar el pago en Webpay. Intenta nuevamente.");
        }
    }

    public PagoResultadoDTO confirmar(String token) {
        Pago pago = pagoRepository.findByWebpayToken(token)
                .orElseThrow(() -> new IllegalArgumentException("No existe un pago asociado al token recibido."));
        if ("AUTHORIZED".equals(pago.getStatus())) {
            return new PagoResultadoDTO(pago.getOrderId(), pago.getStatus(), pago.getAmount(),
                    pago.getAuthorizationCode(), "Pago ya confirmado.");
        }
        try {
            var response = webpay.commit(token);
            boolean authorized = "AUTHORIZED".equals(response.getStatus()) && response.getResponseCode() != null
                    && response.getResponseCode() == 0;
            pago.setStatus(authorized ? "AUTHORIZED" : "REJECTED");
            pago.setAuthorizationCode(response.getAuthorizationCode());
            pago.setResponseCode(response.getResponseCode());
            pagoRepository.save(pago);
            antifraudeService.reiniciarDespuesDeCallback(pago.getClientId());
            eventPublisher.publicar(pago.getOrderId(), pago.getClientId(), pago.getStatus(), pago.getAmount());
            return new PagoResultadoDTO(pago.getOrderId(), pago.getStatus(),
                    response.getAmount() == null ? pago.getAmount() : BigDecimal.valueOf(response.getAmount()),
                    response.getAuthorizationCode(), authorized ? "Pago autorizado." : "El pago fue rechazado.");
        } catch (Exception e) {
            log.error("Error confirmando el pago de la orden {}", pago.getOrderId(), e);
            throw new IllegalStateException("No se pudo confirmar la transacción en Webpay.");
        }
    }

    @Transactional
    public PagoResultadoDTO cancelar(String buyOrder) {
        Pago pago = pagoRepository.findByBuyOrder(buyOrder)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la orden de compra devuelta por Webpay."));
        if (!"AUTHORIZED".equals(pago.getStatus())) {
            pago.setStatus("CANCELLED");
            pagoRepository.save(pago);
            antifraudeService.reiniciarDespuesDeCallback(pago.getClientId());
            eventPublisher.publicar(pago.getOrderId(), pago.getClientId(), pago.getStatus(), pago.getAmount());
        }
        return new PagoResultadoDTO(pago.getOrderId(), pago.getStatus(), pago.getAmount(), null,
                "La transacción fue cancelada en Webpay.");
    }

    public String urlResultado(PagoResultadoDTO result) {
        return frontendUrl + "/pedidos?paymentStatus=" + result.status() + "&orderId=" + result.orderId();
    }
}
