package com.boki0.casino.game.exception;

public class GameSessionAccessDeniedException extends RuntimeException {

    public GameSessionAccessDeniedException() {
        super("Game session does not belong to authenticated player");
    }
}
