package com.ticketchief.common.events;

public record PaymentRequestedEvent(
        String type,
        String correlationId,
        Long orderId,
        long amountCents
) {
    public PaymentRequestedEvent(Long orderId, String correlationId, long amountCents) {
        this("payment.requested", correlationId, orderId, amountCents);
    }
}

