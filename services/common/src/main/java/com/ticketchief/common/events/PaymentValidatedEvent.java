package com.ticketchief.common.events;

import java.util.List;

public record PaymentValidatedEvent(
    String orderId,
    String eventId,
    List<String> seats,
    String userId
) {}

