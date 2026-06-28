package com.boki0.casino.wallet.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateWalletRequest(
        @NotNull UUID authUserId,
        String currency
) {
}
