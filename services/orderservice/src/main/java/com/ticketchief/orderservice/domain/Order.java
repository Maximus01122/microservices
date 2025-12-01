package com.ticketchief.orderservice.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    @JsonCreator
    public Order(@JsonProperty("id") Long id, @JsonProperty("userId") String userId, @JsonProperty("items") List<CartItem> items, @JsonProperty("status") Status status,
                 @JsonProperty("totalAmountCents") long totalAmountCents, @JsonProperty("taxAmountCents") long taxAmountCents, @JsonProperty("currency") String currency,
                 @JsonProperty("createdAt") OffsetDateTime createdAt, @JsonProperty("updatedAt") OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
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

    public boolean assignTicket(String eventId, String seatId, String ticketId, String ticketQr) {
        for (int i = 0; i < items.size(); i++) {
            CartItem current = items.get(i);
            if (Objects.equals(current.eventId(), eventId) && Objects.equals(current.seatId(), seatId)) {
                if (Objects.equals(current.ticketId(), ticketId) && Objects.equals(current.ticketQr(), ticketQr)) {
                    return false;
                }
                CartItem updated = new CartItem(
                        current.id(),
                        current.eventId(),
                        current.seatId(),
                        current.unitPriceCents(),
                        current.reservationId(),
                        ticketId,
                        ticketQr
                );
                items.set(i, updated);
                return true;
            }
        }
        return false;
    }

    public boolean hasAllTicketsIssued() {
        return !items.isEmpty() && items.stream().allMatch(item -> item.ticketQr() != null);
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
