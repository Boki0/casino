package com.boki0.casino.wallet.dto;

import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.entity.WalletTransactionStatus;
import com.boki0.casino.wallet.entity.WalletTransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID walletId,
        UUID authUserId,
        WalletTransactionType type,
        WalletTransactionStatus status,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String currency,
        WalletReferenceType referenceType,
        String referenceId,
        String idempotencyKey,
        String description,
        LocalDateTime createdAt
) {
}
