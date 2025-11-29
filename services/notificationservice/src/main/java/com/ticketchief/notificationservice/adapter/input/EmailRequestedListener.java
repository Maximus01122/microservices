package com.ticketchief.notificationservice.adapter.input;

import com.ticketchief.common.events.EmailSendRequestedEvent;
import com.ticketchief.notificationservice.application.EmailService;
import static org.springframework.amqp.core.Binding.DestinationType.QUEUE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;


@Component
public class EmailRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(EmailRequestedListener.class);
    private final EmailService emailService;

    public EmailRequestedListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.rabbit.notification.queue:notifications.email.send.queue}")
    public void onEmailRequested(EmailSendRequestedEvent ev) {
        log.info("Email request received: to={}, correlationId={}", ev.toEmail(), ev.correlationId());
        emailService.sendInvoice(ev.toEmail(), ev.subject(), ev.bodyText(), ev.invoiceUrlOrPath());
    }
}

