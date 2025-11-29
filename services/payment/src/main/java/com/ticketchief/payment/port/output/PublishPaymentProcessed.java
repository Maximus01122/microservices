package com.ticketchief.payment.port.output;

import com.ticketchief.payment.domain.PaymentRequest;
import com.ticketchief.payment.domain.PaymentResult;

public interface PublishPaymentProcessed {
    void publishProcessed(PaymentRequest request, PaymentResult result);
}
