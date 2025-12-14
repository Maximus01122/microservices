package com.ticketchief.orderservice.domain;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Order domain model.
 */
class OrderTest {

    @Test
    void testOrderCreation() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        assertEquals(1L, order.getId());
        assertEquals("user-123", order.getUserId());
        assertEquals(Order.Status.IN_CART, order.getStatus());
        assertTrue(order.getItems().isEmpty());
    }

    @Test
    void testAddItem() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        CartItem item = new CartItem(1L, "event-1", "A1", 5000L, "res-1", null, null);
        order.addItem(item);
        
        assertEquals(1, order.getItems().size());
        assertEquals("A1", order.getItems().get(0).seatId());
    }

    @Test
    void testTotalAmountCents() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        order.setTotalAmountCents(15000L);
        assertEquals(15000L, order.getTotalAmountCents());
    }

    @Test
    void testAssignTicketSuccess() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1L, "event-1", "A1", 5000L, "res-1", null, null));
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        boolean updated = order.assignTicket("event-1", "A1", "ticket-123", "qr-data");
        
        assertTrue(updated);
        assertEquals("ticket-123", order.getItems().get(0).ticketId());
        assertEquals("qr-data", order.getItems().get(0).ticketQr());
    }

    @Test
    void testAssignTicketNoMatch() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1L, "event-1", "A1", 5000L, "res-1", null, null));
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        // Try to assign to non-matching seat
        boolean updated = order.assignTicket("event-1", "B2", "ticket-123", "qr-data");
        
        assertFalse(updated);
        assertNull(order.getItems().get(0).ticketId());
    }

    @Test
    void testAssignTicketIdempotent() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1L, "event-1", "A1", 5000L, "res-1", "ticket-123", "qr-data"));
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        // Try to assign same ticket again
        boolean updated = order.assignTicket("event-1", "A1", "ticket-123", "qr-data");
        
        assertFalse(updated); // Already has the same values
    }

    @Test
    void testHasAllTicketsIssuedTrue() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1L, "event-1", "A1", 5000L, "res-1", "t1", "qr1"));
        items.add(new CartItem(2L, "event-1", "A2", 5000L, "res-1", "t2", "qr2"));
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        assertTrue(order.hasAllTicketsIssued());
    }

    @Test
    void testHasAllTicketsIssuedFalse() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1L, "event-1", "A1", 5000L, "res-1", "t1", "qr1"));
        items.add(new CartItem(2L, "event-1", "A2", 5000L, "res-1", null, null)); // Missing QR
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        assertFalse(order.hasAllTicketsIssued());
    }

    @Test
    void testHasAllTicketsIssuedEmptyOrder() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        assertFalse(order.hasAllTicketsIssued()); // Empty orders return false
    }

    @Test
    void testMarkPaidFromPaymentPending() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.PAYMENT_PENDING);
        
        order.markPaid();
        
        assertEquals(Order.Status.PAID, order.getStatus());
    }

    @Test
    void testMarkPaidFromWrongStatusThrows() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.IN_CART);
        
        assertThrows(IllegalStateException.class, order::markPaid);
    }

    @Test
    void testCannotAddItemWhenNotInCart() {
        List<CartItem> items = new ArrayList<>();
        Order order = new Order(1L, "user-123", items, Order.Status.PAYMENT_PENDING);
        
        CartItem item = new CartItem(1L, "event-1", "A1", 5000L, "res-1", null, null);
        
        assertThrows(IllegalStateException.class, () -> order.addItem(item));
    }
}
