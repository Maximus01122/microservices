package com.ticketchief.payment.domain;

public class PaymentRequest {
    private final String correlationId;
    private final Long orderId;
    private final long amountCents;
    private final String cardNumber;
    private final String cardCvv;
    private final String cardHolder;

    public PaymentRequest(String correlationId, Long orderId, long amountCents) {
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.cardNumber = null;
        this.cardCvv = null;
        this.cardHolder = null;
    }

    public PaymentRequest(String correlationId, Long orderId, long amountCents, String cardNumber, String cardCvv, String cardHolder) {
        this.correlationId = correlationId;
        this.orderId = orderId;
        this.amountCents = amountCents;
        this.cardNumber = cardNumber;
        this.cardCvv = cardCvv;
        this.cardHolder = cardHolder;
    }

    public String getCorrelationId() { return correlationId; }
    public Long getOrderId() { return orderId; }
    public long getAmountCents() { return amountCents; }
    public String getCardNumber() { return cardNumber; }
    public String getCardCvv() { return cardCvv; }
    public String getCardHolder() { return cardHolder; }
}

