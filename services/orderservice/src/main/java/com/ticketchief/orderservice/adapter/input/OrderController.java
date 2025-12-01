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

    @PostMapping("/finalize/{orderId}")
    public java.util.Map<String, String> finalizeOrder(@PathVariable Long orderId) {
        String correlationId = orderService.finalizeOrder(orderId);
        return java.util.Collections.singletonMap("correlationId", correlationId);
    }

    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> getInvoice(@PathVariable Long orderId) throws IOException {
        return orderService.getInvoice(orderId);
    }

    @DeleteMapping("/remove/{orderId}")
    public Order removeItem(@PathVariable Long orderId, @RequestBody CartItem cartItem) {
        return orderService.removeItem(orderId, cartItem);
    }

    @PutMapping("/add/{orderId}")
    public Order addItem(@PathVariable Long orderId, @RequestBody CartItem cartItem) {
        return orderService.addItem(orderId, cartItem);
    }
}
