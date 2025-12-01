package com.ticketchief.common.events;

public record TicketCreatedEvent(
        String ticketId,
        String orderId,
        String eventId,
        String seat,
        String qr
) {}

