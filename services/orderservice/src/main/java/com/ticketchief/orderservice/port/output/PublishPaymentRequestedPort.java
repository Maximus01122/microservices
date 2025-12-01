package com.ticketchief.orderservice.port.output;

public interface PublishPaymentRequestedPort {
    void publishPaymentRequested(String correlationId, Long orderId, long amountCents);
}
