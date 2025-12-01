package com.ticketchief.orderservice.adapter.output.persistence.entity;

import com.ticketchief.orderservice.domain.Order;
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

    public static OrderEntity fromDomain(Order order) {
        var entity = new OrderEntity();
        entity.setId(order.getId());
        try {
            entity.setUserId(order.getUserId() == null ? null : UUID.fromString(order.getUserId()));
        } catch (IllegalArgumentException ex) {
            entity.setUserId(null);
        }
        entity.setStatus(order.getStatus());
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
                status
        );
    }
}
