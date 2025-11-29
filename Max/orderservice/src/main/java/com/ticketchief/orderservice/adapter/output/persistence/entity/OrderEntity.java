package com.ticketchief.orderservice.adapter.output.persistence.entity;

import com.ticketchief.orderservice.domain.Order;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
public class OrderEntity {
    @Id
    @GeneratedValue
    private Long id;
    private Long userId;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<CartItemEntity> items;

    private Order.Status status;


    public void setId(Long id) {
        this.id = id;
    }

    public void setUserId(Long userId) {
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
        entity.setUserId(order.getUserId());
        entity.setStatus(order.getStatus());
        entity.setItems(order.getItems().stream().map(CartItemEntity::fromDomain).toList());
        return entity;
    }

    public Order toDomain() {
        return new Order(
                id, userId,
                items.stream().map(CartItemEntity::toDomain).collect(Collectors.toCollection(ArrayList::new)),
                status
        );
    }
}
