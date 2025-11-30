package com.ticketchief.notificationservice.application;

import com.ticketchief.notificationservice.port.output.InvoiceFetcherPort;
import com.ticketchief.notificationservice.port.output.SendEmailPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final SendEmailPort sendEmailPort;
    private final InvoiceFetcherPort invoiceFetcherPort;

    public EmailService(SendEmailPort sendEmailPort, InvoiceFetcherPort invoiceFetcherPort) {
        this.sendEmailPort = sendEmailPort;
        this.invoiceFetcherPort = invoiceFetcherPort;
    }

    public void sendInvoice(String to, String subject, String body, String invoiceUrlOrPath) {
        try {
            String fileName = "invoice.pdf";
            byte[] bytes = invoiceFetcherPort.fetch(invoiceUrlOrPath);
            sendEmailPort.send(to, subject, body, bytes, fileName);

            log.info("Email sent to {} with attachment {}", to, fileName);

        } catch (Exception e) {
            log.error("Failed to send invoice email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void sendVerification(String to, String token) {
        try {
            String subject = "Please verify your TicketChief account";
            String verificationLink = String.format("%s/verify?token=%s", "https://example.com/verify", token);
            String body = "Welcome to TicketChief!\n\nPlease verify your email by clicking the link below:\n" + verificationLink;
            sendEmailPort.send(to, subject, body, null, null);
            log.info("Verification email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}

