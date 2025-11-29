package com.ticketchief.orderservice.adapter.output.persistence.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.ArrayList;
import java.util.List;

// CartItemEntity.java
@Entity
public class CartItemEntity {
    @Id
    @GeneratedValue
    private Long id;
    private Long eventId;
    private Long seatId;
    private long unitPriceCents;

    public static CartItemEntity fromDomain(CartItem cartItem) {
        CartItemEntity e = new CartItemEntity();
        e.setId(cartItem.id());
        e.setEventId(cartItem.eventId());
        e.setSeatId(cartItem.seatId());
        e.setUnitPriceCents(cartItem.unitPriceCents());
        return e;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public void setUnitPriceCents(long unitPriceCents) {
        this.unitPriceCents = unitPriceCents;
    }

    public CartItem toDomain() {
        return new CartItem(id, eventId, seatId, unitPriceCents);
    }

}
