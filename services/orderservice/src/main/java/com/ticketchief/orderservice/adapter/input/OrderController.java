package com.ticketchief.orderservice.adapter.input;

import com.ticketchief.orderservice.application.OrderService;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {
        return orderService.placeOrder(order);
    }

    @DeleteMapping("/{orderId}")
    public void cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
    }

    @GetMapping("/{orderId}")
    public Order findOrder(@PathVariable Long orderId) {
        return orderService.findOrder(orderId);
    }

    @PostMapping("/{orderId}/payments")
    public java.util.Map<String, String> initiatePayment(@PathVariable Long orderId) {
        String correlationId = orderService.finalizeOrder(orderId);
        return java.util.Collections.singletonMap("correlationId", correlationId);
    }

    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> getInvoice(@PathVariable Long orderId) throws IOException {
        return orderService.getInvoice(orderId);
    }
}
