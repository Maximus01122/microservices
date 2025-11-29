package com.ticketchief.payment.adapter.input;

import com.ticketchief.common.events.PaymentRequestedEvent;
import com.ticketchief.payment.domain.PaymentRequest;
import com.ticketchief.payment.port.input.ProcessPayment;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentRequestedListener {

    private final ProcessPayment processPayment;

    public PaymentRequestedListener(ProcessPayment processPayment) {
        this.processPayment = processPayment;
    }

    @RabbitListener(queues = "${app.rabbit.requested.queue:payments.requested.queue}")
    public void onMessage(PaymentRequestedEvent requestedEvent) {
        PaymentRequest request = new PaymentRequest(requestedEvent.correlationId(), requestedEvent.orderId(), requestedEvent.amountCents());
        processPayment.process(request);
    }
}

