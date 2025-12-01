package com.ticketchief.orderservice.port.output;

import java.util.List;

public interface PublishPaymentValidatedPort {
    void publishPaymentValidated(String orderId, String eventId, List<String> seats, String userId, String reservationId);
}

