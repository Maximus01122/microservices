package com.ticketchief.orderservice.port.output;

import com.ticketchief.orderservice.domain.Order;

public interface PublishPaymentRequestedPort {
    void publishPaymentRequested(String correlationId, Long orderId, long amountCents);
}
