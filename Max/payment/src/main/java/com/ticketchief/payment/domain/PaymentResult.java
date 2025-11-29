package com.ticketchief.payment.domain;

import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;

public class PaymentResult {
    private final PaymentStatus status;
    private final String reason;

    public PaymentResult(PaymentStatus status, String reason) {
        this.status = status;
        this.reason = reason;
    }
    public PaymentStatus getStatus() { return status; }
    public String getReason() { return reason; }
}
