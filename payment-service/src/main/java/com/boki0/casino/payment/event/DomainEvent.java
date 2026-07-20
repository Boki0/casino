package com.boki0.casino.payment.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID eventId();

    String eventType();

    int eventVersion();

    Instant occurredAt();
}
