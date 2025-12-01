package com.ticketchief.orderservice.adapter.output.persistence.entity;

import com.ticketchief.orderservice.domain.CartItem;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

// CartItemEntity.java
@Entity
public class CartItemEntity {
    @Id
    @GeneratedValue
    private Long id;
    private String eventId;
    private String seatId;
    private long unitPriceCents;
    private String reservationId;

    public static CartItemEntity fromDomain(CartItem cartItem) {
        CartItemEntity e = new CartItemEntity();
        e.setId(cartItem.id());
        e.setEventId(cartItem.eventId());
        e.setSeatId(cartItem.seatId());
        e.setUnitPriceCents(cartItem.unitPriceCents());
        e.setReservationId(cartItem.reservationId());
        return e;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public void setSeatId(String seatId) {
        this.seatId = seatId;
    }

    public void setUnitPriceCents(long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
    }

    public CartItem toDomain() {
        return new CartItem(id, eventId, seatId, unitPriceCents, reservationId);
    }

}
