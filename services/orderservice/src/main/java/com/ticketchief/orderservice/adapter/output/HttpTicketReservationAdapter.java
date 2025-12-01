package com.ticketchief.orderservice.adapter.output;

import com.ticketchief.orderservice.port.output.TicketReservationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class HttpTicketReservationAdapter implements TicketReservationPort {

    private final RestTemplate rest;
    private final String eventTicketBase;

    public HttpTicketReservationAdapter(@Value("${app.event-ticket.base:http://event-ticket:3001}") String eventTicketBase) {
        this.rest = new RestTemplate();
        this.eventTicketBase = eventTicketBase;
    }

    @Override
    public ReservationResult reserveSeats(String ownerId, List<SeatRef> seats, long ttlSeconds) {
        throw new UnsupportedOperationException("reserveSeats not implemented in HTTP adapter");
    }

    @Override
    public boolean confirmReservation(String reservationId) {
        throw new UnsupportedOperationException("confirmReservation not implemented");
    }

    @Override
    public boolean releaseReservation(String reservationId) {
        try {
            String url = eventTicketBase + "/reservations/" + reservationId + "/release";
            ResponseEntity<String> res = rest.postForEntity(url, null, String.class);
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            return false;
        }
    }
}
