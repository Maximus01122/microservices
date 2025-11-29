package com.ticketchief.notificationservice.port.output;

public interface SendEmailPort {
    void send(String to, String subject, String bodyText, byte[] pdfBytes, String attachmentFilename);
}
