package com.ticketchief.orderservice.adapter.input;

import com.ticketchief.common.events.PaymentProcessedEvent;
import com.ticketchief.orderservice.port.input.OrderPaymentServicePort;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentProcessedListener {
    private final OrderPaymentServicePort orderPaymentServicePort;

    public PaymentProcessedListener(OrderPaymentServicePort orderPaymentServicePort, RabbitTemplate rabbitTemplate) {
        this.orderPaymentServicePort = orderPaymentServicePort;
    }

    @RabbitListener(queues = "payment.processed.queue")
    public void onPaymentProcessed(PaymentProcessedEvent event) {
        orderPaymentServicePort.onPaymentProcessed(event);
    }
}
