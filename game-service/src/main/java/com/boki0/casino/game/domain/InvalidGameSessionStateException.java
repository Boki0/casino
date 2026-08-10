package com.boki0.casino.game.domain;

public class InvalidGameSessionStateException extends RuntimeException {

    private InvalidGameSessionStateException(String message) {
        super(message);
    }

    public static InvalidGameSessionStateException forOperation(
            GameSessionStatus currentStatus,
            String operation
    ) {
        return new InvalidGameSessionStateException(
                "Cannot " + operation + " game session from status " + currentStatus
        );
    }

    public static InvalidGameSessionStateException forOperation(
            GameSessionStatus currentStatus,
            String operation,
            String reason
    ) {
        return new InvalidGameSessionStateException(
                "Cannot " + operation + " game session from status " + currentStatus + ": " + reason
        );
    }
}
