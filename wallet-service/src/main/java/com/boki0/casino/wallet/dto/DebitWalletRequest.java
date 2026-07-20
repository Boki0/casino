package com.boki0.casino.wallet.dto;

import com.boki0.casino.wallet.entity.WalletReferenceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record DebitWalletRequest(
        @NotNull UUID authUserId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull WalletReferenceType referenceType,
        @NotBlank String referenceId,
        @NotBlank String idempotencyKey,
        String description
) {
}
