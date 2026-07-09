package com.boki0.casino.payment.provider;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateCheckoutCommand(
        UUID depositOrderId,
        UUID authUserId,
        BigDecimal amount,
        String currency,
        BigDecimal creditsAmount,
        String idempotencyKey,
        String successUrl,
        String cancelUrl
) {
}
