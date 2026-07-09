package com.boki0.casino.wallet.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentDepositCompletedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID depositOrderId,
        UUID authUserId,
        BigDecimal amount,
        String currency,
        BigDecimal creditsAmount,
        String provider,
        Instant occurredAt
) {
}
