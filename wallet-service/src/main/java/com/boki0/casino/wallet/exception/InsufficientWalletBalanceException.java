package com.boki0.casino.wallet.exception;

public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException() {
        super("Insufficient wallet balance");
    }
}
