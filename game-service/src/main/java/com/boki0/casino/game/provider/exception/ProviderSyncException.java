package com.boki0.casino.game.provider.exception;

public class ProviderSyncException extends RuntimeException {

    private final ProviderSyncErrorType errorType;

    private ProviderSyncException(String message, ProviderSyncErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    private ProviderSyncException(String message, ProviderSyncErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public static ProviderSyncException invalidProviderData(String message) {
        return new ProviderSyncException(message, ProviderSyncErrorType.INVALID_PROVIDER_DATA);
    }

    public static ProviderSyncException persistenceFailure(String message, Throwable cause) {
        return new ProviderSyncException(message, ProviderSyncErrorType.PERSISTENCE_FAILURE, cause);
    }

    public ProviderSyncErrorType getErrorType() {
        return errorType;
    }

    public enum ProviderSyncErrorType {
        INVALID_PROVIDER_DATA,
        PERSISTENCE_FAILURE
    }
}
