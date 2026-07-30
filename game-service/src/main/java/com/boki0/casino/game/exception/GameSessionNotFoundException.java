package com.boki0.casino.game.exception;

import java.util.UUID;

public class GameSessionNotFoundException extends RuntimeException {

    private GameSessionNotFoundException(String message) {
        super(message);
    }

    public static GameSessionNotFoundException byId(UUID id) {
        return new GameSessionNotFoundException("Game session not found for id: " + id);
    }
}
