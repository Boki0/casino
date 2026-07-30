package com.boki0.casino.game.exception;

import java.util.UUID;

public class GameUnavailableException extends RuntimeException {

    private GameUnavailableException(String message) {
        super(message);
    }

    public static GameUnavailableException disabled(UUID gameId) {
        return new GameUnavailableException("Game is disabled for id: " + gameId);
    }

    public static GameUnavailableException providerUnavailable(UUID gameId) {
        return new GameUnavailableException("Game provider is unavailable for game id: " + gameId);
    }
}
