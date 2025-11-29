package com.ticketchief.orderservice.domain;

import java.util.Collections;
import java.util.List;

public class Order {

    private final Long id;
    private final Long userId;
    private final List<CartItem> items;
    private Status status;
    public enum Status {
        IN_CART,
        PAYMENT_PENDING,
        PAYMENT_FAILED,
        PAID}

    public Order(Long id, Long userId, List<CartItem> items, Status status) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.status = status == null ? Status.IN_CART : status;
    }


    public Long getUserId() {
        return userId;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public void addItem(CartItem item) {
        requireEditable();
        items.add(item);
    }

    public void removeItem(CartItem item) {
        requireEditable();
        items.remove(item);
    }

    public Long getId() {
        return id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public void markPaid() {
        if (status != Status.PAYMENT_PENDING) {
            throw new IllegalStateException("Cannot mark PAID from " + status);
        }
        this.status = Status.PAID;
    }

    private void requireEditable() {
        if (this.status != Status.IN_CART) throw new IllegalStateException("Order is frozen (" + status + ")");
    }
}
