package com.ticketchief.notificationservice.adapter.input;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ticketchief.common.events.UserVerificationRequestedEvent;
import org.springframework.amqp.core.Message;
import com.ticketchief.notificationservice.application.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserVerificationListener {

    private static final Logger log = LoggerFactory.getLogger(UserVerificationListener.class);
    private final EmailService emailService;

    public UserVerificationListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.rabbit.notification.verification-queue:email.verification.queue}")
    public void onUserVerificationRequested(Message raw) {
        try {
            String body = new String(raw.getBody(), raw.getMessageProperties().getContentEncoding() != null ? raw.getMessageProperties().getContentEncoding() : "UTF-8");
            log.info("Raw verification message received: routingKey={}, body={}", raw.getMessageProperties().getReceivedRoutingKey(), body);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(body);

            // Try common property names used by publishers
            String userId = node.has("userId") ? node.get("userId").asText() : node.path("user_id").asText(null);
            String email = node.has("email") ? node.get("email").asText() : node.path("to").asText(null);
            String token = node.has("token") ? node.get("token").asText() : node.path("verificationToken").asText(null);

            if (email == null || token == null) {
                log.warn("Verification message missing email or token — userId={} email={} token={}", userId, email, token);
                return;
            }

            log.info("Parsed verification event: userId={}, email={}, token={}", userId, email, token);
            emailService.sendVerification(email, token);
        } catch (Exception e) {
            log.error("Failed to process verification message: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
