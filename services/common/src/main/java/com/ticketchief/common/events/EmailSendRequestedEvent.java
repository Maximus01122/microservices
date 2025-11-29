package com.ticketchief.common.events;


public record EmailSendRequestedEvent(
        String type,
        String correlationId,
        String toEmail,
        String subject,
        String bodyText,
        String invoiceUrlOrPath
) {
    public EmailSendRequestedEvent(String correlationId, String toEmail, String subject, String bodyText, String invoiceUrlOrPath) {
        this("email.send", correlationId, toEmail, subject, bodyText, invoiceUrlOrPath);
    }
}

