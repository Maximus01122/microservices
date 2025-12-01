package com.ticketchief.orderservice.domain;

public record CartItem(
        Long id,
        String eventId,
        String seatId,
        long unitPriceCents,
        String reservationId,
        String ticketId,
        String ticketQr
) {
}
