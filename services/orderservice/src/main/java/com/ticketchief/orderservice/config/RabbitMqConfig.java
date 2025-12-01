package com.ticketchief.orderservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {


    @Value("${app.rabbit.payment.exchange:payments.exchange}")
    private String paymentsExchange;

    @Value("${app.rabbit.payment.requested.routing-key:payment.requested}")
    private String paymentsRoutingKey;



    @Value("${app.rabbit.payment.processed.routing-key:payment.processed}")
    private String paymentsProcessedRoutingKey;

    @Value("${app.rabbit.payment.processed.queue:payment.processed.queue}")
    private String processedQueueName;
    public static final String PAYMENT_PROCESSED_QUEUE = "payment.processed.queue";

    @Value("${app.rabbit.ticket.created.queue:ticket.created.queue}")
    private String ticketCreatedQueueName;

    @Value("${app.rabbit.ticket.created.routing-key:ticket.created}")
    private String ticketCreatedRoutingKey;

    @Bean
    public TopicExchange paymentsExchange() {
        return ExchangeBuilder.topicExchange(paymentsExchange).durable(true).build();
    }

    @Bean
    public Queue paymentsProcessedQueue() {
        return QueueBuilder.durable(processedQueueName).build();
    }

    @Bean
    public Queue ticketCreatedQueue() {
        return QueueBuilder.durable(ticketCreatedQueueName).build();
    }


    @Bean
    public Binding bindProcessedQueue(Queue paymentsProcessedQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(paymentsProcessedQueue).to(paymentsExchange).with(paymentsProcessedRoutingKey);
    }

    @Bean
    public Binding bindTicketCreatedQueue(Queue ticketCreatedQueue, TopicExchange paymentsExchange) {
        return BindingBuilder.bind(ticketCreatedQueue).to(paymentsExchange).with(ticketCreatedRoutingKey);
    }

    @Bean
    public PaymentRabbitProperties paymentRabbitProperties() {
        return new PaymentRabbitProperties(paymentsExchange, paymentsRoutingKey);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate rt = new RabbitTemplate(connectionFactory);
        rt.setMessageConverter(converter);
        return rt;
    }
}
