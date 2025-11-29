package com.ticketchief.payment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${app.rabbit.exchange:payments.exchange}")
    private String exchangeName;

    @Value("${app.rabbit.requested.queue:payments.requested.queue}")
    private String requestQueueName;

    @Value("${app.rabbit.requested.routing-key:payment.requested}")
    private String requestRoutingKey;

    @Bean
    public TopicExchange paymentsExchange() {
        return ExchangeBuilder.topicExchange(exchangeName).durable(true).build();
    }

    @Bean
    public Queue paymentsRequestQueue() {
        return QueueBuilder.durable(requestQueueName).build();
    }

    @Bean
    public Binding requestBinding(Queue paymentsRequestQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentsRequestQueue).to(paymentsExchange).with(requestRoutingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, Jackson2JsonMessageConverter converter) {
        RabbitTemplate rt = new RabbitTemplate(cf);
        rt.setMessageConverter(converter);
        rt.setMandatory(true); // helpful for returns if unroutable
        return rt;
    }
}

