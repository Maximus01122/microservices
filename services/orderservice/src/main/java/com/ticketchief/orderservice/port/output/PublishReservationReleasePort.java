package com.ticketchief.orderservice.port.output;

public interface PublishReservationReleasePort {
    void publishReservationRelease(String reservationId, String orderId);
}
