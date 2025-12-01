package com.ticketchief.orderservice.application;

import com.ticketchief.common.events.PaymentProcessedEvent;
import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;
import com.ticketchief.common.events.TicketCreatedEvent;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;
import com.ticketchief.orderservice.domain.Order.Status;
import com.ticketchief.orderservice.port.input.OrderPaymentServicePort;
import com.ticketchief.orderservice.port.input.OrderServicePort;
import com.ticketchief.orderservice.port.output.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Objects;

import static com.ticketchief.orderservice.domain.Order.Status.PAYMENT_PENDING;


@Service
public class OrderService implements OrderServicePort, OrderPaymentServicePort {
    private final OrdersRepositoryPort ordersJpaAdapter;
    private final PublishPaymentRequestedPort paymentPublisher;
    private final PublishEmailRequestedPort emailPublisher;
    private final PublishPaymentValidatedPort paymentValidatedPublisher;
    private final InvoicePort invoiceAdapter;

    @Value("${app.invoice.storage-dir}")
    private String storageDir;

    public OrderService(OrdersRepositoryPort ordersJpaAdapter,
                        PublishPaymentRequestedPort paymentPublisher,
                        PublishEmailRequestedPort emailPublisher,
                        PublishPaymentValidatedPort paymentValidatedPublisher,
                        InvoicePort invoiceAdapter) {
        this.ordersJpaAdapter = ordersJpaAdapter;
        this.paymentPublisher = paymentPublisher;
        this.emailPublisher = emailPublisher;
        this.paymentValidatedPublisher = paymentValidatedPublisher;
        this.invoiceAdapter = invoiceAdapter;
    }
    @Override
    public Order placeOrder(Order order) {
        return ordersJpaAdapter.save(order);
    }

    public Order addItem(Long orderId, CartItem item) {
        Order existingOrder = ordersJpaAdapter.findOrderById(orderId);
        existingOrder.addItem(item);
        return ordersJpaAdapter.save(existingOrder);
    }

    public Order removeItem(Long orderId, CartItem item) {
        Order existingOrder = ordersJpaAdapter.findOrderById(orderId);
        existingOrder.removeItem(item);
        return ordersJpaAdapter.save(existingOrder);
    }

    @Override
    public Order findOrder(Long orderId) {
        return ordersJpaAdapter.findOrderById(orderId);
    }

    @Override
    public ResponseEntity<byte[]> getInvoice(Long orderId) throws IOException {
        Order order = ordersJpaAdapter.findOrderById(orderId);
        Path pdfPath = Paths.get(storageDir).resolve(order.getId() + ".pdf").toAbsolutePath();
        if (!Files.exists(pdfPath)) {
            return ResponseEntity.notFound().build();
        }

        byte[] pdfBytes = Files.readAllBytes(pdfPath);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + order.getId() + ".pdf\"")
                .body(pdfBytes);
    }

    @Override
    public void cancelOrder(Long orderId) {
        ordersJpaAdapter.deleteById(orderId);
    }

    @Override
    public void finalizeOrder(Long orderId) {
        Order order = ordersJpaAdapter.findOrderById(orderId);

        long amount = order.getItems().stream().mapToLong(CartItem::unitPriceCents).sum();
        double HST_ONTARIO = 0.14;
        amount += (long) (amount * HST_ONTARIO);

        order.setStatus(PAYMENT_PENDING);
        ordersJpaAdapter.save(order);
        paymentPublisher.publishPaymentRequested(UUID.randomUUID().toString(), order.getId(), amount);
    }


    @Transactional
    @Override
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        Order order = ordersJpaAdapter.findOrderById(event.orderId());
        if (event.status() == PaymentStatus.SUCCESS) {
            order.markPaid();

            // Group items by eventId to publish validation per event
            Map<String, List<CartItem>> itemsByEvent = order.getItems().stream()
                    .collect(Collectors.groupingBy(CartItem::eventId));

            itemsByEvent.forEach((eventId, items) -> {
                List<String> seats = items.stream().map(CartItem::seatId).toList();
                // derive reservationId from items if present
                String reservationId = items.stream().map(CartItem::reservationId).filter(Objects::nonNull).findFirst().orElse(null);
                paymentValidatedPublisher.publishPaymentValidated(
                        String.valueOf(order.getId()),
                        eventId,
                        seats,
                        order.getUserId(),
                        reservationId
                );
            });

        } else {
            order.setStatus(Status.FAILED);
        }
        ordersJpaAdapter.save(order);
    }

    public void onTicketCreated(TicketCreatedEvent event) {
        if (event.orderId() == null) {
            return;
        }
        Long orderId;
        try {
            orderId = Long.valueOf(event.orderId());
        } catch (NumberFormatException ex) {
            return;
        }
        Order order = ordersJpaAdapter.findOrderById(orderId);
        boolean updated = order.assignTicket(event.eventId(), event.seat(), event.ticketId(), event.qr());
        if (!updated) {
            return;
        }
        ordersJpaAdapter.save(order);

        if (order.hasAllTicketsIssued()) {
            InvoicePort.InvoiceResult result = invoiceAdapter.generateInvoice(order);
            emailPublisher.publishEmailRequest(
                    UUID.randomUUID().toString(),
                    "fuchsm1@mcmaster.ca",
                    "Your Invoice",
                    "Thank you for your purchase. Your tickets are attached as QR codes.",
                    result.url()
            );
        }
    }
}
