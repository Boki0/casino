package com.boki0.casino.wallet.dto;

import com.boki0.casino.wallet.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID authUserId,
        BigDecimal balance,
        String currency,
        WalletStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
