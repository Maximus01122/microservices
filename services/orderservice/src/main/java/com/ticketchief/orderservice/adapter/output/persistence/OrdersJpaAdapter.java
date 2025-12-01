package com.ticketchief.orderservice.adapter.output.persistence;

import com.ticketchief.orderservice.adapter.output.persistence.entity.OrderEntity;
import com.ticketchief.orderservice.adapter.output.persistence.entity.OrderJpaRepository;
import com.ticketchief.orderservice.domain.Order;
import com.ticketchief.orderservice.port.output.OrdersRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class OrdersJpaAdapter implements OrdersRepositoryPort {
    private final OrderJpaRepository orderJpaRepository;

    public OrdersJpaAdapter(OrderJpaRepository orderJpaRepository) {
        this.orderJpaRepository = orderJpaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findOrderById(Long orderId) throws RuntimeException {
        return orderJpaRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId)).toDomain();
    }

    @Override
    public Order save(Order order) {
        // ensure timestamps are set so DB NOT NULL columns are populated
        if (order.getCreatedAt() == null) {
            order.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        }
        order.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return orderJpaRepository.save(OrderEntity.fromDomain(order)).toDomain();
    }

    @Override
    public void deleteById(Long orderId) {
        orderJpaRepository.deleteById(orderId);
    }
}
