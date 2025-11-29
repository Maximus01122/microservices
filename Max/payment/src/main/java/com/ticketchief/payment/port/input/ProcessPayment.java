package com.ticketchief.payment.port.input;

import com.ticketchief.payment.domain.PaymentRequest;

public interface ProcessPayment {
    void process(PaymentRequest request);
}
