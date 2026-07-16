package com.boki0.casino.notification.email.smtp;

import com.boki0.casino.notification.email.EmailSender;
import com.boki0.casino.notification.email.SendEmailCommand;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender javaMailSender;
    private final String fromAddress;

    public SmtpEmailSender(
            JavaMailSender javaMailSender,
            @Value("${notification.mail.from}") String fromAddress
    ) {
        this.javaMailSender = javaMailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(SendEmailCommand command) {
        validate(command);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(command.recipient());
        message.setSubject(command.subject());
        message.setText(command.body());

        javaMailSender.send(message);
    }

    private void validate(SendEmailCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Email command must not be null");
        }
        if (command.recipient() == null || command.recipient().isBlank()) {
            throw new IllegalArgumentException("Email recipient must not be blank");
        }
        if (command.subject() == null || command.subject().isBlank()) {
            throw new IllegalArgumentException("Email subject must not be blank");
        }
        if (command.body() == null) {
            throw new IllegalArgumentException("Email body must not be null");
        }
        if (fromAddress == null || fromAddress.isBlank()) {
            throw new IllegalStateException("Notification sender address is not configured");
        }
    }
}
