package com.ticketchief.orderservice.port.output;

public interface PublishEmailRequestedPort {
    void publishEmailRequest(String correlationId, String toEmail, String subject, String body, String invoiceUrlOrPath);
}
