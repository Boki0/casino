package com.boki0.casino.payment.history;

import com.boki0.casino.payment.entity.PaymentProviderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentHistoryItemResponse(
        UUID id,
        PaymentHistoryType type,
        BigDecimal amount,
        String currency,
        PaymentHistoryStatus status,
        PaymentProviderType provider,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
}
