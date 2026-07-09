package com.boki0.casino.payment.event;

import com.boki0.casino.payment.entity.PaymentProviderType;
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
        PaymentProviderType provider,
        Instant occurredAt
) implements DomainEvent {

    public static final String EVENT_TYPE = "PAYMENT_DEPOSIT_COMPLETED";
    public static final int EVENT_VERSION = 1;
}
