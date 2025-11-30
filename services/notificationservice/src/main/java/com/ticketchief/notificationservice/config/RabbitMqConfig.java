package com.ticketchief.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Value("${app.rabbit.notification.exchange:ticketchief}")
    private String exchangeName;

    @Value("${app.rabbit.notification.queue:email.send.queue}")
    private String queueName;

    @Value("${app.rabbit.notification.routing-key:email.send}")
    private String routingKey;

    @Value("${app.rabbit.notification.verification-queue:email.verification.queue}")
    private String verificationQueueName;

    @Value("${app.rabbit.notification.verification-routing-key:user.email.verification.requested}")
    private String verificationRoutingKey;

    @Bean
    public TopicExchange notificationsExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Queue notificationsQueue() {
        return QueueBuilder.durable(queueName).build();
    }

    @Bean
    public Binding notificationsBinding(Queue notificationsQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(notificationsQueue).to(notificationsExchange).with(routingKey);
    }

    @Bean
    public Queue verificationQueue() {
        return QueueBuilder.durable(verificationQueueName).build();
    }

    @Bean
    public Binding verificationBinding(Queue verificationQueue, TopicExchange notificationsExchange) {
        return BindingBuilder.bind(verificationQueue).to(notificationsExchange).with(verificationRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        rabbitTemplate.setMandatory(true); // ensures unroutable messages are returned
        return rabbitTemplate;
    }
}
