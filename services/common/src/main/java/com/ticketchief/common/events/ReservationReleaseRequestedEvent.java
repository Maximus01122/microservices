package com.ticketchief.common.events;

public record ReservationReleaseRequestedEvent(
    String reservationId,
    String orderId
) {
}

