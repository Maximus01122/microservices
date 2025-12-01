package com.ticketchief.payment.domain;

import java.time.Instant;

public class PaymentSession {
    private final String correlationId;
    private final Long orderId;
    private final long amountCents;
    private final String status;
    private final Instant createdAt;

    public PaymentSession(String correlationId, Long orderId, long amountCents, String status, Instant createdAt) {
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getCorrelationId() { return correlationId; }
    public Long getOrderId() { return orderId; }
    public long getAmountCents() { return amountCents; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
