package com.ticketchief.orderservice.config;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    private final AmqpAdmin amqpAdmin;

    public RabbitConfig(AmqpAdmin amqpAdmin) {
        this.amqpAdmin = amqpAdmin;
    }

    @Bean
    public TopicExchange notificationExchange(@Value("${app.rabbit.notification.exchange:email.exchange}") String exchangeName) {
        TopicExchange ex = new TopicExchange(exchangeName, true, false);
        // Ensure the exchange exists on the broker at startup
        amqpAdmin.declareExchange(ex);
        return ex;
    }
}
