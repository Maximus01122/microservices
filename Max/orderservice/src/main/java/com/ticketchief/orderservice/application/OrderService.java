package com.ticketchief.orderservice.application;

import com.ticketchief.common.events.PaymentProcessedEvent;
import com.ticketchief.orderservice.domain.CartItem;
import com.ticketchief.orderservice.domain.Order;
import com.ticketchief.orderservice.port.input.OrderPaymentServicePort;
import com.ticketchief.orderservice.port.input.OrderServicePort;
import com.ticketchief.orderservice.port.output.InvoicePort;
import com.ticketchief.orderservice.port.output.OrdersRepositoryPort;
import com.ticketchief.orderservice.port.output.PublishEmailRequestedPort;
import com.ticketchief.orderservice.port.output.PublishPaymentRequestedPort;

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
import java.util.UUID;

import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;

import com.ticketchief.orderservice.domain.Order.Status;

import static com.ticketchief.orderservice.domain.Order.Status.PAYMENT_PENDING;


@Service
public class OrderService implements OrderServicePort, OrderPaymentServicePort {
    private final OrdersRepositoryPort ordersJpaAdapter;
    private final PublishPaymentRequestedPort paymentPublisher;
    private final PublishEmailRequestedPort emailPublisher;
    private final InvoicePort invoiceAdapter;

    @Value("${app.invoice.storage-dir}")
    private String storageDir;

    public OrderService(OrdersRepositoryPort ordersJpaAdapter, PublishPaymentRequestedPort paymentPublisher, PublishEmailRequestedPort emailPublisher, InvoicePort invoiceAdapter) {
        this.ordersJpaAdapter = ordersJpaAdapter;
        this.paymentPublisher = paymentPublisher;
        this.emailPublisher = emailPublisher;
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
        // reservation to ticket management microservice
        try {
            //ticketReservationPort.reserveSeats();
        } catch (Exception e) {
            throw new RuntimeException("Seats not available anymore! Choose another seat");
        }

        long amount = order.getItems().stream().mapToLong(CartItem::unitPriceCents).sum();
        double HST_ONTARIO = 0.14;
        amount += amount * HST_ONTARIO;

        order.setStatus(PAYMENT_PENDING);
        ordersJpaAdapter.save(order);
        paymentPublisher.publishPaymentRequested(UUID.randomUUID().toString(), order.getId(), amount);
    }


    @Transactional
    @Override
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        Order order = ordersJpaAdapter.findOrderById(event.orderId());
        if (event.status() == PaymentStatus.SUCCESS) {
            System.out.println("fetch user data from user service");
            // ask costumer service for user details
            InvoicePort.InvoiceResult result = invoiceAdapter.generateInvoice(order);
            //ticketReservationPort.confirmSeats(...);
            order.markPaid();
            //eventPublisher.publishOrderCompleted(...);
            // send mail to costumer
            // Create email event


            emailPublisher.publishEmailRequest(UUID.randomUUID().toString(), "fuchsm1@mcmaster.ca", result.url());
            System.out.println("send invoice to customer");

        } else {
            order.setStatus(Status.PAYMENT_FAILED);
            //ticketReservationPort.releaseSeats(...);
        }
        ordersJpaAdapter.save(order);
    }
}
