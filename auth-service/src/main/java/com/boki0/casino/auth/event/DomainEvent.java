package com.boki0.casino.auth.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    String eventType();

    int eventVersion();

    LocalDateTime occurredAt();
}
