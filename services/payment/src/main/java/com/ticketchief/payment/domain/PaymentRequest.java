package com.ticketchief.payment.domain;

public class PaymentRequest {
    private final String correlationId;
    private final Long orderId;
    private final long amountCents;

    public PaymentRequest(String correlationId, Long orderId, long amountCents) {
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.amountCents = amountCents;
    }

    public String getCorrelationId() { return correlationId; }
    public Long getOrderId() { return orderId; }
    public long getAmountCents() { return amountCents; }
}

