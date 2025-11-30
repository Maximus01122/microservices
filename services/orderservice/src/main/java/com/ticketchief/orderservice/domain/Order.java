package com.ticketchief.orderservice.domain;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

public class Order {

    private final Long id;
    private final String userId;
    private final List<CartItem> items;
    private Status status;
    private long totalAmountCents;
    private long taxAmountCents;
    private String currency;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public enum Status {
        IN_CART,
        PAYMENT_PENDING,
        PAID,
        FAILED
    }

    public Order(Long id, String userId, List<CartItem> items, Status status) {
        this(id, userId, items, status, 0L, 0L, "CAD", null, null);
    }

    public Order(Long id, String userId, List<CartItem> items, Status status,
                 long totalAmountCents, long taxAmountCents, String currency,
                 OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.items = items;
        this.status = status == null ? Status.IN_CART : status;
        this.totalAmountCents = totalAmountCents;
        this.taxAmountCents = taxAmountCents;
        this.currency = currency == null ? "CAD" : currency;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getUserId() {
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

    public long getTotalAmountCents() {
        return totalAmountCents;
    }

    public void setTotalAmountCents(long totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public long getTaxAmountCents() {
        return taxAmountCents;
    }

    public void setTaxAmountCents(long taxAmountCents) {
        this.taxAmountCents = taxAmountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
