package cl.proyectoEcommerce.ms_pagos.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pagos", uniqueConstraints = {
        @UniqueConstraint(name = "uk_pagos_order_id", columnNames = "order_id"),
        @UniqueConstraint(name = "uk_pagos_buy_order", columnNames = "buy_order")
})
public class Pago {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "client_id", nullable = false, length = 150)
    private String clientId;
    @Column(name = "buy_order", nullable = false, length = 26)
    private String buyOrder;
    @Column(name = "session_id", nullable = false, length = 61)
    private String sessionId;
    @Column(nullable = false, precision = 12, scale = 0)
    private BigDecimal amount;
    @Column(name = "webpay_token", length = 64)
    private String webpayToken;
    @Column(name = "webpay_url", length = 500)
    private String webpayUrl;
    @Column(nullable = false, length = 24)
    private String status;
    @Column(name = "authorization_code", length = 20)
    private String authorizationCode;
    @Column(name = "response_code")
    private Integer responseCode;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pago() {}
    public Pago(Long orderId, String clientId, String buyOrder, String sessionId, BigDecimal amount) {
        this.orderId = orderId; this.clientId = clientId; this.buyOrder = buyOrder;
        this.sessionId = sessionId; this.amount = amount; this.status = "CREATING";
        this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
    @PreUpdate void updateTimestamp() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getClientId() { return clientId; }
    public String getBuyOrder() { return buyOrder; }
    public String getSessionId() { return sessionId; }
    public BigDecimal getAmount() { return amount; }
    public String getWebpayToken() { return webpayToken; }
    public String getWebpayUrl() { return webpayUrl; }
    public String getStatus() { return status; }
    public String getAuthorizationCode() { return authorizationCode; }
    public Integer getResponseCode() { return responseCode; }
    public void setWebpayToken(String webpayToken) { this.webpayToken = webpayToken; }
    public void setWebpayUrl(String webpayUrl) { this.webpayUrl = webpayUrl; }
    public void reiniciarTransaccion(String buyOrder, String sessionId, BigDecimal amount) {
        this.buyOrder = buyOrder; this.sessionId = sessionId; this.amount = amount;
        this.webpayToken = null; this.webpayUrl = null; this.authorizationCode = null; this.responseCode = null;
        this.status = "CREATING"; this.updatedAt = Instant.now();
    }
    public void setStatus(String status) { this.status = status; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
}
