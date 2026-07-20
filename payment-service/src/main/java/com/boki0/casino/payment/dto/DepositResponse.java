package com.boki0.casino.payment.dto;

import com.boki0.casino.payment.entity.DepositStatus;
import com.boki0.casino.payment.entity.PaymentProviderType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepositResponse(
        UUID depositId,
        UUID authUserId,
        BigDecimal amount,
        String currency,
        BigDecimal creditsAmount,
        DepositStatus status,
        PaymentProviderType provider,
        String providerSessionId,
        String providerPaymentId,
        String checkoutUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {
}
