package com.ticketchief.orderservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

public class PaymentRabbitProperties {
    private final String exchange;
    private final String routingKey;
    public PaymentRabbitProperties(String exchange, String routingKey) {
        this.exchange = exchange; this.routingKey = routingKey;
    }
    public String getExchange() { return exchange; }
    public String getRoutingKey() { return routingKey; }
}
