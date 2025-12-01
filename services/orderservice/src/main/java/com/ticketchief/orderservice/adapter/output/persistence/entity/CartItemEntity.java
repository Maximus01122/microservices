package com.ticketchief.orderservice.adapter.output.persistence.entity;

import com.ticketchief.orderservice.domain.CartItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.*;
import java.util.UUID;

// CartItemEntity.java
@Entity
@Table(name = "order_items")
public class CartItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "seat_id")
    private String seatId;

    @Column(name = "unit_price_cents")
    private long unitPriceCents;

    @Column(name = "ticket_id")
    private UUID ticketId;

    @Column(length = 4096)
    private String ticketQr;

    @Column(name = "reservation_id")
    private UUID reservationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    public static CartItemEntity fromDomain(CartItem cartItem) {
        CartItemEntity e = new CartItemEntity();
        e.setId(cartItem.id());
        try {
            e.setEventId(cartItem.eventId() == null ? null : UUID.fromString(cartItem.eventId()));
        } catch (IllegalArgumentException ex) {
            e.setEventId(null);
        }
        e.setSeatId(cartItem.seatId());
        e.setUnitPriceCents(cartItem.unitPriceCents());
        try {
            e.setTicketId(cartItem.ticketId());
        } catch (IllegalArgumentException ex) {
            e.setTicketId((String) null);
        }
        e.setTicketQr(cartItem.ticketQr());
        try {
            e.setReservationId(cartItem.reservationId() == null ? null : UUID.fromString(cartItem.reservationId()));
        } catch (IllegalArgumentException ex) {
            e.setReservationId(null);
        }
        return e;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public void setUnitPriceCents(long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public void setOrder(OrderEntity order) {
        this.order = order;
    }

    public void setTicketId(String ticketId) {
        try {
            this.ticketId = ticketId == null ? null : UUID.fromString(ticketId);
        } catch (IllegalArgumentException ex) {
            this.ticketId = null;
        }
    }

    public void setTicketQr(String ticketQr) {
        this.ticketQr = ticketQr;
    }

    public CartItem toDomain() {
        return new CartItem(id, eventId == null ? null : eventId.toString(), seatId, unitPriceCents, reservationId == null ? null : reservationId.toString(), ticketId == null ? null : ticketId.toString(), ticketQr);
    }
}
