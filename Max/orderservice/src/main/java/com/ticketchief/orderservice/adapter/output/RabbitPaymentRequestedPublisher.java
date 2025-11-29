package com.ticketchief.orderservice.adapter.output;


import com.ticketchief.common.events.PaymentRequestedEvent;
import com.ticketchief.orderservice.config.PaymentRabbitProperties;
import com.ticketchief.orderservice.port.output.PublishPaymentRequestedPort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class RabbitPaymentRequestedPublisher implements PublishPaymentRequestedPort {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentRabbitProperties props;


    public RabbitPaymentRequestedPublisher(RabbitTemplate rabbitTemplate, PaymentRabbitProperties props) {
        this.rabbitTemplate = rabbitTemplate;
        this.props = props;
    }

    @Override
    public void publishPaymentRequested(String correlationId, Long orderId, long amountCents) {
        PaymentRequestedEvent dto = new PaymentRequestedEvent(
                orderId,
                correlationId,
                amountCents
        );

        // convertAndSend will use the Jackson2JsonMessageConverter configured on RabbitTemplate
        rabbitTemplate.convertAndSend(props.getExchange(), props.getRoutingKey(), dto);
    }
}

