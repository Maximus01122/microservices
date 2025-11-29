package com.ticketchief.orderservice.domain;

public record CartItem(Long id, Long eventId, Long seatId, long unitPriceCents) {
}
