package com.ticketchief.common.events;

public record PaymentProcessedEvent(
        String type,
        String correlationId,
        Long orderId,
        PaymentStatus status,
        String reason
) {
    public enum PaymentStatus { SUCCESS, FAILED }

    public PaymentProcessedEvent(String correlationId, Long orderId, PaymentStatus status, String reason) {
        this("payment.processed", correlationId, orderId, status, reason);
    }
}

