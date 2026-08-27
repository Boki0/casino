package com.boki0.casino.wallet.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletDepositCreditedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID depositOrderId,
        UUID authUserId,
        UUID walletTransactionId,
        BigDecimal amount,
        String currency,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String status,
        Instant occurredAt
) {

    public static final String EVENT_TYPE = "WALLET_DEPOSIT_CREDITED";
    public static final int EVENT_VERSION = 1;
}
