package com.boki0.casino.game.exception;

import java.util.UUID;

public class GameNotFoundException extends RuntimeException {

    private GameNotFoundException(String message) {
        super(message);
    }

    public static GameNotFoundException byId(UUID id) {
        return new GameNotFoundException("Game not found for id: " + id);
    }

    public static GameNotFoundException bySlug(String slug) {
        return new GameNotFoundException("Game not found for slug: " + slug);
    }
}
