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
        void finalizeOrder(Long orderId);
        Order addItem(Long orderId, CartItem item);
        Order removeItem(Long orderId, CartItem item);
        Order findOrder(Long orderId);

        ResponseEntity<byte[]> getInvoice(Long orderId) throws IOException;

}
