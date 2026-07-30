package com.boki0.casino.game.wallet.exception;

public class WalletClientException extends RuntimeException {

    public enum Category {
        UNAVAILABLE,
        INVALID_RESPONSE,
        CURRENCY_MISMATCH
    }

    private final Category category;

    public WalletClientException(String message) {
        this(message, Category.UNAVAILABLE);
    }

    public WalletClientException(String message, Throwable cause) {
        this(message, cause, Category.UNAVAILABLE);
    }

    public WalletClientException(String message, Category category) {
        super(message);
        this.category = category;
    }

    public WalletClientException(String message, Throwable cause, Category category) {
        super(message, cause);
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }
}
