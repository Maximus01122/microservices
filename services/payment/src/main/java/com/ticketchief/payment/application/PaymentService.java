package com.ticketchief.payment.application;

import com.ticketchief.common.events.PaymentProcessedEvent.PaymentStatus;

import com.ticketchief.payment.domain.PaymentRequest;
import com.ticketchief.payment.domain.PaymentResult;
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
    private final Random random = new Random();
    private final double successRate;
    private final long simulatedDelayMs;

    public PaymentService(PublishPaymentProcessed publisher,
                          @Value("${app.payment.simulator.success-rate:0.95}") double successRate,
                          @Value("${app.payment.simulator.delay-ms:500}") long simulatedDelayMs) {
        this.publisher = publisher;
        this.successRate = successRate;
        this.simulatedDelayMs = simulatedDelayMs;
    }

    @Override
    public void process(PaymentRequest request) {
        log.info("Processing payment request: orderId={}, correlationId={}", request.getOrderId(), request.getCorrelationId());

        // simulate latency
        try { Thread.sleep(simulatedDelayMs); } catch (InterruptedException ignored) {}

        boolean success = random.nextDouble() < successRate;
        PaymentResult result = success
                ? new PaymentResult(PaymentStatus.SUCCESS, null)
                : new PaymentResult(PaymentStatus.FAILED, "simulated-decline");

        log.info("Payment result for orderId={} -> {}", request.getOrderId(), result.getStatus());
        // publish processed event
        publisher.publishProcessed(request, result);
    }
}

