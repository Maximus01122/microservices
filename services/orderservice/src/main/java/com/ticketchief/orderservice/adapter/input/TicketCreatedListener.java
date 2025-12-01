package com.ticketchief.orderservice.adapter.input;

import com.ticketchief.common.events.TicketCreatedEvent;
import com.ticketchief.orderservice.application.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class TicketCreatedListener {
    private final OrderService orderService;

    public TicketCreatedListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = "${app.rabbit.ticket.created.queue:ticket.created.queue}")
    public void handleTicketCreated(TicketCreatedEvent event) {
        orderService.onTicketCreated(event);
    }
}

