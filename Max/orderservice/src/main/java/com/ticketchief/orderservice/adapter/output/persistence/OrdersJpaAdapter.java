package com.ticketchief.orderservice.adapter.output.persistence;

import com.ticketchief.orderservice.adapter.output.persistence.entity.OrderEntity;
import com.ticketchief.orderservice.adapter.output.persistence.entity.OrderJpaRepository;
import com.ticketchief.orderservice.domain.Order;
import com.ticketchief.orderservice.port.output.OrdersRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
        return orderJpaRepository.save(OrderEntity.fromDomain(order)).toDomain();
    }

    @Override
    public void deleteById(Long orderId) {
        orderJpaRepository.deleteById(orderId);
    }
}
