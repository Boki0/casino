package com.boki0.casino.user.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID authUserId,
        String email,
        String username,
        LocalDateTime occurredAt
) {
}
