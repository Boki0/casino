package com.boki0.casino.wallet.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    private WalletNotFoundException(String message) {
        super(message);
    }

    public static WalletNotFoundException forPlayerAndCurrency(UUID playerId, String currency) {
        return new WalletNotFoundException(
                "Wallet not found for playerId " + playerId + " and currency " + currency
        );
    }
}
