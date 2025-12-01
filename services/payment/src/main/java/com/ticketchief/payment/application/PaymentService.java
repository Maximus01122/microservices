package com.ticketchief.payment.application;

import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;

import com.ticketchief.payment.domain.PaymentRequest;
import com.ticketchief.payment.domain.PaymentResult;
import com.ticketchief.payment.domain.PaymentSession;
import com.ticketchief.payment.adapter.output.PaymentSessionRepository;
import com.ticketchief.payment.adapter.output.TransactionRepository;
import com.ticketchief.payment.port.input.ProcessPayment;
import com.ticketchief.payment.port.output.PublishPaymentProcessed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class PaymentService implements ProcessPayment {

    private final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PublishPaymentProcessed publisher;
    private final TransactionRepository txRepo;
    private final PaymentSessionRepository sessionRepo;
    private final Random random = new Random();
    private final double successRate;
    private final long simulatedDelayMs;

    public PaymentService(PublishPaymentProcessed publisher,
                          TransactionRepository txRepo,
                          PaymentSessionRepository sessionRepo,
                          @Value("${app.payment.simulator.success-rate:0.95}") double successRate,
                          @Value("${app.payment.simulator.delay-ms:500}") long simulatedDelayMs) {
        this.publisher = publisher;
        this.txRepo = txRepo;
        this.sessionRepo = sessionRepo;
        this.successRate = successRate;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    @Override
    public void process(PaymentRequest request) {
        log.info("Creating payment session: orderId={}, correlationId={}", request.getOrderId(), request.getCorrelationId());
        PaymentSession session = new PaymentSession(request.getCorrelationId(), request.getOrderId(), request.getAmountCents(), "PENDING", null);
        try {
            sessionRepo.insertSession(session);
        } catch (Exception e) {
            log.warn("Failed to persist payment session: {}", e.getMessage());
        }
    }

    /**
     * Perform a single payment attempt using card credentials provided in the request.
     * Returns the PaymentResult for this single attempt and publishes a PaymentProcessedEvent
     * when the payment is finally succeeded or exhausted (3 attempts).
     */
    public PaymentResult attempt(PaymentRequest request) {
        log.info("Attempting payment: orderId={}, correlationId={}", request.getOrderId(), request.getCorrelationId());

        final String cardNumber = request.getCardNumber();
        if (cardNumber != null && cardNumber.equals("666")) {
            log.info("Card declined by rule for orderId={} (card is 666)", request.getOrderId());
            PaymentResult result = new PaymentResult(PaymentStatus.FAILED, "declined-by-rule");
            try {
                txRepo.insertTransaction(request.getCorrelationId(), request.getOrderId(), null, request.getAmountCents(), result.getStatus().name(), "declined-by-rule");
            } catch (Exception e) {
                log.warn("Failed to persist transaction: {}", e.getMessage());
            }
            // Immediately mark session as FAILED and publish final event
            try {
                sessionRepo.updateStatus(request.getCorrelationId(), "FAILED");
            } catch (Exception e) {
                log.warn("Failed to update session status: {}", e.getMessage());
            }
            publisher.publishProcessed(request, result);
            return result;
        }

        try { Thread.sleep(simulatedDelayMs); } catch (InterruptedException ignored) {}

        boolean success = random.nextDouble() < successRate;
        PaymentResult result = success
                ? new PaymentResult(PaymentStatus.SUCCESS, null)
                : new PaymentResult(PaymentStatus.FAILED, "simulated-decline");

        try {
            txRepo.insertTransaction(request.getCorrelationId(), request.getOrderId(), null, request.getAmountCents(), result.getStatus().name(), result.getReason());
        } catch (Exception e) {
            log.warn("Failed to persist transaction attempt: {}", e.getMessage());
        }

        int attempts = txRepo.countAttempts(request.getCorrelationId());
        if (result.getStatus() == PaymentStatus.SUCCESS) {
            sessionRepo.updateStatus(request.getCorrelationId(), "SUCCESS");
            publisher.publishProcessed(request, result);
        } else if (attempts >= 3) {
            sessionRepo.updateStatus(request.getCorrelationId(), "FAILED");
            publisher.publishProcessed(request, result);
        }

        log.info("Payment attempt result for orderId={} -> {} (attempt #{})", request.getOrderId(), result.getStatus(), attempts);
        return result;
    }
}

