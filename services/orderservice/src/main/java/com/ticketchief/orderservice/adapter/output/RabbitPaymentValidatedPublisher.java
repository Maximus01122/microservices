package com.ticketchief.orderservice.adapter.output;

import com.ticketchief.common.events.PaymentValidatedEvent;
import com.ticketchief.orderservice.port.output.PublishPaymentValidatedPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RabbitPaymentValidatedPublisher implements PublishPaymentValidatedPort {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitPaymentValidatedPublisher(RabbitTemplate rabbitTemplate,
                                           @Value("${app.rabbit.payment.exchange:ticketchief}") String exchange,
                                           @Value("${app.rabbit.payment.validated.routing-key:payment.validated}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishPaymentValidated(String orderId, String eventId, List<String> seats, String userId) {
        PaymentValidatedEvent event = new PaymentValidatedEvent(
                orderId, eventId, seats, userId
        );
        rabbitTemplate.convertAndSend(exchange, routingKey, event);
    }
}

