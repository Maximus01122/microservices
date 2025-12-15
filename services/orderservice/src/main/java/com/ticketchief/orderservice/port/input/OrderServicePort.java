package com.ticketchief.orderservice.port.input;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;

public interface OrderServicePort {
        Order placeOrder(Order order);
        void cancelOrder(Long orderId);
        String finalizeOrder(Long orderId);
        Order findOrder(Long orderId);

        // Add/remove items from an order
        Order addItem(Long orderId, com.ticketchief.orderservice.domain.CartItem item);
        void deleteItem(Long orderId, Long itemId);

        ResponseEntity<byte[]> getInvoice(Long orderId) throws IOException;

}
