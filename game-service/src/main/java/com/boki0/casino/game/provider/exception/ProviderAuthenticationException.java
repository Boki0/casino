package com.boki0.casino.game.provider.exception;

import org.springframework.http.HttpStatus;

public class ProviderAuthenticationException extends RuntimeException {

    private final HttpStatus status;
    private final int errorCode;
    private final String publicDescription;

    private ProviderAuthenticationException(
            HttpStatus status,
            int errorCode,
            String publicDescription
    ) {
        super(publicDescription);
        this.status = status;
        this.errorCode = errorCode;
        this.publicDescription = publicDescription;
    }

    public static ProviderAuthenticationException rejected() {
        return new ProviderAuthenticationException(
                HttpStatus.UNAUTHORIZED,
                1001,
                "Session authentication failed"
        );
    }

    public static ProviderAuthenticationException walletCurrencyMismatch() {
        return new ProviderAuthenticationException(
                HttpStatus.CONFLICT,
                1002,
                "Wallet currency does not match session currency"
        );
    }

    public static ProviderAuthenticationException walletUnavailable() {
        return new ProviderAuthenticationException(
                HttpStatus.BAD_GATEWAY,
                2001,
                "Wallet service is unavailable"
        );
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getPublicDescription() {
        return publicDescription;
    }
}
