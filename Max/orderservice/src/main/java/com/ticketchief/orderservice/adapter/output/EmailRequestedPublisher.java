package com.ticketchief.orderservice.adapter.output;

import com.ticketchief.common.events.EmailSendRequestedEvent;
import com.ticketchief.orderservice.port.output.PublishEmailRequestedPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailRequestedPublisher implements PublishEmailRequestedPort {

    private static final Logger log = LoggerFactory.getLogger(EmailRequestedPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public EmailRequestedPublisher(
            RabbitTemplate rabbitTemplate,
            @Value("${app.rabbit.notification.exchange:notifications.exchange}") String exchange,
            @Value("${app.rabbit.notification.routing-key:email.send}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishEmailRequest(String correlationId, String toEmail, String invoiceUrlOrPath) {
        EmailSendRequestedEvent emailEvent = new EmailSendRequestedEvent(
                correlationId,
                toEmail,
                "Your TicketChief Invoice",
                "Thank you for your purchase! Please find your invoice attached.",
                invoiceUrlOrPath
        );

        log.info("Publishing EmailSendRequestedEvent for {}", emailEvent.toEmail());
        rabbitTemplate.convertAndSend(exchange, routingKey, emailEvent);
    }
}