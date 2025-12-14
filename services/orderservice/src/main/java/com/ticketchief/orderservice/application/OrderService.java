package com.ticketchief.orderservice.application;

import com.ticketchief.common.events.PaymentProcessedEvent;
import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;
import com.ticketchief.common.events.TicketCreatedEvent;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;
import com.ticketchief.orderservice.domain.Order.Status;
import com.ticketchief.orderservice.port.input.OrderServicePort;
import com.ticketchief.orderservice.port.input.OrderPaymentServicePort;
import com.ticketchief.orderservice.port.output.OrdersRepositoryPort;
import com.ticketchief.orderservice.port.output.PublishEmailRequestedPort;
import com.ticketchief.orderservice.port.output.PublishPaymentRequestedPort;
import com.ticketchief.orderservice.port.output.PublishPaymentValidatedPort;
import com.ticketchief.orderservice.port.output.InvoicePort;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OrderService implements OrderServicePort, OrderPaymentServicePort {
    private final Logger log = LoggerFactory.getLogger(OrderService.class);
    private final OrdersRepositoryPort ordersJpaAdapter;
    private final PublishPaymentRequestedPort paymentPublisher;
    private final PublishEmailRequestedPort emailPublisher;
    private final com.ticketchief.orderservice.port.output.UserClientPort userClient;
    private final PublishPaymentValidatedPort paymentValidatedPublisher;
    private final InvoicePort invoiceAdapter;
    private final com.ticketchief.orderservice.port.output.TicketReservationPort ticketReservationPort;

    @Value("${app.invoice.storage-dir}")
    private String storageDir;

    public OrderService(OrdersRepositoryPort ordersJpaAdapter,
                        PublishPaymentRequestedPort paymentPublisher,
                        PublishEmailRequestedPort emailPublisher,
                        com.ticketchief.orderservice.port.output.UserClientPort userClient,
                        PublishPaymentValidatedPort paymentValidatedPublisher,
                        InvoicePort invoiceAdapter,
                        com.ticketchief.orderservice.port.output.TicketReservationPort ticketReservationPort) {
        this.ordersJpaAdapter = ordersJpaAdapter;
        this.paymentPublisher = paymentPublisher;
        this.emailPublisher = emailPublisher;
        this.userClient = userClient;
        this.paymentValidatedPublisher = paymentValidatedPublisher;
        this.invoiceAdapter = invoiceAdapter;
        this.ticketReservationPort = ticketReservationPort;
    }

    @Override
    public Order placeOrder(Order order) {
        return ordersJpaAdapter.save(order);
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
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + order.getId() + "\"")
                .body(pdfBytes);
    }

    @Override
    public void cancelOrder(Long orderId) {
        ordersJpaAdapter.deleteById(orderId);
    }

    @Override
    public String finalizeOrder(Long orderId) {
        // publish payment requested and return correlationId so frontend can submit card data to payment service
        Order order = ordersJpaAdapter.findOrderById(orderId);

        long amount = order.getItems().stream().mapToLong(CartItem::unitPriceCents).sum();
        double HST_ONTARIO = 0.14;
        amount += (long) (amount * HST_ONTARIO);

        order.setStatus(Status.PAYMENT_PENDING);
        ordersJpaAdapter.save(order);

        String correlationId = UUID.randomUUID().toString();
        paymentPublisher.publishPaymentRequested(
                correlationId,
                order.getId(),
                amount
        );
        return correlationId;
    }

    @Transactional
    @Override
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        Order order = ordersJpaAdapter.findOrderById(event.orderId());
        if (event.status() == PaymentStatus.SUCCESS) {
            // idempotency: if we already marked this order PAID, ignore duplicate SUCCESS events
            if (order.getStatus() == Status.PAID) {
                log.info("Ignoring duplicate PAYMENT SUCCESS for orderId={}", event.orderId());
                return;
            }
            // if order is not currently awaiting payment, log and ignore to avoid invalid state transitions
            if (order.getStatus() != Status.PAYMENT_PENDING) {
                log.warn("Received PAYMENT SUCCESS for orderId={} but order status is {} — ignoring", event.orderId(), order.getStatus());
                return;
            }

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
            // Payment failed (final): log, release reservation and delete order
            log.warn("Received PAYMENT FAILED for orderId={} (reason={}), correlationId={}", event.orderId(), event.reason(), event.correlationId());
            String reservationId = order.getItems().stream().map(CartItem::reservationId).filter(Objects::nonNull).findFirst().orElse(null);
            try {
                if (reservationId != null) {
                    ticketReservationPort.releaseReservation(reservationId);
                }
            } catch (Exception ex) {
                // log and continue
            }
            ordersJpaAdapter.deleteById(order.getId());
            return;
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
            // Resolve user email synchronously via userClient (outside port)
            String recipient = null;
            try {
                recipient = userClient.getUserEmail(order.getUserId());
            } catch (Exception ex) {
                log.warn("Failed to resolve user email for userId={}: {}", order.getUserId(), ex.getMessage());
            }
            if (recipient == null || recipient.isBlank()) {
                log.warn("No valid email for userId={} — cannot send invoice for orderId={}", order.getUserId(), order.getId());
                return;
            }

            emailPublisher.publishEmailRequest(
                UUID.randomUUID().toString(),
                recipient,
                "Your Invoice",
                "Thank you for your purchase. Your tickets are attached as QR codes.",
                result.url()
            );
        }
    }
}
