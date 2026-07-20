package com.boki0.casino.notification.email;

public record SendEmailCommand(
        String recipient,
        String subject,
        String body
) {
}
