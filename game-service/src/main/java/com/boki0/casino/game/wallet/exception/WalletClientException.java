package com.boki0.casino.game.wallet.exception;

public class WalletClientException extends RuntimeException {

    public WalletClientException(String message) {
        super(message);
    }

    public WalletClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
