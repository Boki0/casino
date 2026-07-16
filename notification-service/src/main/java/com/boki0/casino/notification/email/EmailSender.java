package com.boki0.casino.notification.email;

public interface EmailSender {

    void send(SendEmailCommand command);
}
