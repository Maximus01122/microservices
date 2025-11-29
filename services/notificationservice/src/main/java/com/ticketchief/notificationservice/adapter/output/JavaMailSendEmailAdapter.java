package com.ticketchief.notificationservice.adapter.output;


import com.ticketchief.notificationservice.port.output.SendEmailPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class JavaMailSendEmailAdapter implements SendEmailPort {

    private final JavaMailSender mailSender;

    public JavaMailSendEmailAdapter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void send(String to, String subject, String bodyText, byte[] pdfBytes, String attachmentFilename) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, pdfBytes != null);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(bodyText, false); // plain text; set true for HTML
            helper.setFrom("no-reply@ticketchief.com");

            if (pdfBytes != null) {
                helper.addAttachment(attachmentFilename, new ByteArrayResource(pdfBytes));
            }
            mailSender.send(msg);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to send mail", e);
        }
    }
}

