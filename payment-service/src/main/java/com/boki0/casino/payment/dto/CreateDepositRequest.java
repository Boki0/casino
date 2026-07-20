package com.boki0.casino.payment.dto;

import com.boki0.casino.payment.entity.PaymentProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CreateDepositRequest(
        @NotNull(message = "amount is required")
        @Positive(message = "amount must be positive")
        BigDecimal amount,

        @NotBlank(message = "currency is required")
        String currency,

        @NotNull(message = "creditsAmount is required")
        @Positive(message = "creditsAmount must be positive")
        BigDecimal creditsAmount,

        @NotNull(message = "provider is required")
        PaymentProviderType provider,

        @NotBlank(message = "idempotencyKey is required")
        String idempotencyKey,

        String successUrl,

        String cancelUrl
) {
}
