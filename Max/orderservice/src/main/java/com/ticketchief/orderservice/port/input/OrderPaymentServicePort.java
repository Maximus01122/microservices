package com.ticketchief.orderservice.port.input;


import com.ticketchief.common.events.PaymentProcessedEvent;

public interface OrderPaymentServicePort {
        void onPaymentProcessed(PaymentProcessedEvent event);
}
