package com.ticketchief.orderservice.port.output;

import java.util.List;

public interface TicketReservationPort {
    ReservationResult reserveSeats(String ownerId, List<SeatRef> seats, long ttlSeconds);
    boolean confirmReservation(String reservationId);
    boolean releaseReservation(String reservationId);

    record SeatRef(Long eventId, Long seatId) {}
    record ReservationResult(boolean success, String reservationId, List<SeatRef> failedSeats) {}
}
