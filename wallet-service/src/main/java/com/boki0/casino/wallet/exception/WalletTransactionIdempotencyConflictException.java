package com.boki0.casino.wallet.exception;

public class WalletTransactionIdempotencyConflictException extends RuntimeException {

    public WalletTransactionIdempotencyConflictException(String externalReference) {
        super("External reference has already been used for different transaction data: "
                + externalReference);
    }
}
