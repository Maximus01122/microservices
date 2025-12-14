package com.ticketchief.orderservice.adapter.output;

import com.ticketchief.common.events.ReservationReleaseRequestedEvent;
import com.ticketchief.orderservice.port.output.PublishReservationReleasePort;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RabbitReservationReleasePublisher implements PublishReservationReleasePort {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public RabbitReservationReleasePublisher(RabbitTemplate rabbitTemplate,
                                             @Value("${app.rabbit.exchange:ticketchief}") String exchange,
                                             @Value("${app.rabbit.reservation.release.routing-key:reservation.release}") String routingKey) {
        this.rabbitTemplate = rabbitTemplate;
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    @Override
    public void publishReservationRelease(String reservationId, String orderId) {
        ReservationReleaseRequestedEvent ev = new ReservationReleaseRequestedEvent(reservationId, orderId);
        rabbitTemplate.convertAndSend(exchange, routingKey, ev);
    }
}
