package com.ticketchief.payment.domain;

import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;
import com.ticketchief.payment.application.PaymentService;
import com.ticketchief.payment.adapter.output.PaymentSessionRepository;
import com.ticketchief.payment.adapter.output.TransactionRepository;
import com.ticketchief.payment.port.output.PublishPaymentProcessed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Payment Service.
 * Tests cover payment processing logic including the 666 card failure case.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentSessionRepository sessionRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PublishPaymentProcessed publisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        // 100% success rate for deterministic testing (except 666 card)
        paymentService = new PaymentService(publisher, transactionRepository, sessionRepository, 1.0, 0);
    }

    @Test
    void testPaymentRequestCreation() {
        PaymentRequest request = new PaymentRequest(
            "corr-123",
            456L,
            10000L,
            "4242424242424242",
            "123",
            "John Doe"
        );

        assertEquals("corr-123", request.getCorrelationId());
        assertEquals(456L, request.getOrderId());
        assertEquals(10000L, request.getAmountCents());
        assertEquals("4242424242424242", request.getCardNumber());
    }

    @Test
    void testPaymentWithCard666AlwaysFails() {
        PaymentRequest request = new PaymentRequest(
            "corr-666",
            666L,
            5000L,
            "666",  // Known bad card number
            "123",
            "Fraud Test"
        );

        PaymentResult result = paymentService.attempt(request);

        // Card 666 should always fail
        assertEquals(PaymentStatus.FAILED, result.getStatus());
        assertEquals("declined-by-rule", result.getReason());
        
        // Verify publisher was called
        verify(publisher).publishProcessed(eq(request), any(PaymentResult.class));
    }

    @Test
    void testPaymentSessionCreation() {
        PaymentSession session = new PaymentSession("test-corr-id", 123L, 15000L, "PENDING", null);

        assertEquals("test-corr-id", session.getCorrelationId());
        assertEquals(123L, session.getOrderId());
        assertEquals(15000L, session.getAmountCents());
        assertEquals("PENDING", session.getStatus());
    }

    @Test
    void testPaymentResultStatuses() {
        PaymentResult success = new PaymentResult(PaymentStatus.SUCCESS, null);
        PaymentResult failed = new PaymentResult(PaymentStatus.FAILED, "Card declined");

        assertEquals(PaymentStatus.SUCCESS, success.getStatus());
        assertNull(success.getReason());

        assertEquals(PaymentStatus.FAILED, failed.getStatus());
        assertEquals("Card declined", failed.getReason());
    }

    @Test
    void testSuccessfulPayment() {
        when(transactionRepository.countAttempts("corr-success")).thenReturn(1);

        PaymentRequest request = new PaymentRequest(
            "corr-success",
            789L,
            5000L,
            "4242424242424242",
            "123",
            "John Doe"
        );

        PaymentResult result = paymentService.attempt(request);

        // With 100% success rate, should succeed
        assertEquals(PaymentStatus.SUCCESS, result.getStatus());
        
        // Verify session status was updated
        verify(sessionRepository).updateStatus("corr-success", "SUCCESS");
    }

    @Test
    void testProcessCreatesSession() {
        PaymentRequest request = new PaymentRequest(
            "corr-new",
            999L,
            7500L
        );

        paymentService.process(request);

        // Verify session was inserted
        verify(sessionRepository).insertSession(any(PaymentSession.class));
    }
}
