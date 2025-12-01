package com.ticketchief.orderservice.adapter.output.persistence.entity;

import com.ticketchief.orderservice.domain.Order;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_id_seq_gen")
    @SequenceGenerator(name = "orders_id_seq_gen", sequenceName = "orders_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id")
    private UUID userId;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItemEntity> items;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private Order.Status status;

    @Column(name = "total_amount")
    private long totalAmountCents;

    @Column(name = "tax_amount")
    private long taxAmountCents;

    @Column(name = "currency")
    private String currency;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;


    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public void setItems(List<CartItemEntity> items) {
        this.items = items;
    }


    public void setStatus(Order.Status status) {
        this.status = status;
    }

    public void setTotalAmountCents(long totalAmountCents) {
        this.totalAmountCents = totalAmountCents;
    }

    public void setTaxAmountCents(long taxAmountCents) {
        this.taxAmountCents = taxAmountCents;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static OrderEntity fromDomain(Order order) {
        var entity = new OrderEntity();
        entity.setId(order.getId());
        try {
            entity.setUserId(order.getUserId() == null ? null : UUID.fromString(order.getUserId()));
        } catch (IllegalArgumentException ex) {
            entity.setUserId(null);
        }
        entity.setStatus(order.getStatus());
        entity.setTotalAmountCents(order.getTotalAmountCents());
        entity.setTaxAmountCents(order.getTaxAmountCents());
        entity.setCurrency(order.getCurrency());
        entity.setCreatedAt(order.getCreatedAt() == null ? null : order.getCreatedAt());
        entity.setUpdatedAt(order.getUpdatedAt() == null ? null : order.getUpdatedAt());
        var items = order.getItems().stream().map(CartItemEntity::fromDomain).toList();
        // set back-reference so JPA will populate the order_id FK correctly
        items.forEach(i -> i.setOrder(entity));
        entity.setItems(items);
        return entity;
    }

    public Order toDomain() {
        return new Order(
            id, userId == null ? null : userId.toString(),
            items.stream().map(CartItemEntity::toDomain).collect(Collectors.toCollection(ArrayList::new)),
            status,
            totalAmountCents,
            taxAmountCents,
            currency,
            createdAt,
            updatedAt
        );
    }
}
