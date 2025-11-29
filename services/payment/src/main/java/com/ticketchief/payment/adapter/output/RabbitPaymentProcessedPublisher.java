package com.ticketchief.payment.adapter.output;

import com.ticketchief.common.events.PaymentProcessedEvent;
import com.ticketchief.payment.domain.PaymentRequest;
import com.ticketchief.payment.domain.PaymentResult;
import com.ticketchief.payment.port.output.PublishPaymentProcessed;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitPaymentProcessedPublisher implements PublishPaymentProcessed {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitPaymentProcessedPublisher(RabbitTemplate rabbitTemplate,
                                           @Value("${app.rabbit.processed.exchange:payments.exchange}") String exchange,
                                           @Value("${app.rabbit.processed.routing-key:payment.processed}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishProcessed(PaymentRequest request, PaymentResult result) {
        PaymentProcessedEvent dto = new PaymentProcessedEvent(
                request.getCorrelationId(),
                request.getOrderId(),
                result.getStatus(),
                result.getReason()
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, dto);
    }
}

