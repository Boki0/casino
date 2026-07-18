package com.boki0.casino.game.provider.exception;

public class GameSyncException extends RuntimeException {

    private final GameSyncErrorType errorType;

    private GameSyncException(String message, GameSyncErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    private GameSyncException(String message, GameSyncErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public static GameSyncException invalidGameData(String message) {
        return new GameSyncException(message, GameSyncErrorType.INVALID_GAME_DATA);
    }

    public static GameSyncException invalidGameData(String message, Throwable cause) {
        return new GameSyncException(message, GameSyncErrorType.INVALID_GAME_DATA, cause);
    }

    public static GameSyncException missingProvider(String message) {
        return new GameSyncException(message, GameSyncErrorType.MISSING_PROVIDER);
    }

    public static GameSyncException persistenceFailure(String message, Throwable cause) {
        return new GameSyncException(message, GameSyncErrorType.PERSISTENCE_FAILURE, cause);
    }

    public GameSyncErrorType getErrorType() {
        return errorType;
    }

    public enum GameSyncErrorType {
        INVALID_GAME_DATA,
        MISSING_PROVIDER,
        PERSISTENCE_FAILURE
    }
}
