package com.ticketchief.orderservice.port.output;

import com.ticketchief.orderservice.domain.Order;

public interface OrdersRepositoryPort {
    Order findOrderById(Long orderId) throws RuntimeException;
    Order save(Order order);
    void deleteById(Long orderId);
}
