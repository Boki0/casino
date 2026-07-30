package com.boki0.casino.game.service;

import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.exception.GameSessionNotFoundException;
import com.boki0.casino.game.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@Service
public class GameSessionLifecycleService {

    private final GameSessionRepository gameSessionRepository;

    public GameSessionLifecycleService(GameSessionRepository gameSessionRepository) {
        this.gameSessionRepository = gameSessionRepository;
    }

    @Transactional
    public void activate(UUID sessionId, String providerSessionId) {
        GameSession session = findSession(sessionId);
        session.activate(providerSessionId);
    }

    @Transactional
    public void markFailed(UUID sessionId) {
        GameSession session = findSession(sessionId);
        session.markFailed();
    }

    private GameSession findSession(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return gameSessionRepository.findById(sessionId)
                .orElseThrow(() -> GameSessionNotFoundException.byId(sessionId));
    }
}
