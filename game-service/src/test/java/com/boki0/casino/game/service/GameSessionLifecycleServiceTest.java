package com.boki0.casino.game.service;

import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.exception.GameSessionNotFoundException;
import com.boki0.casino.game.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameSessionLifecycleServiceTest {

    private GameSessionRepository gameSessionRepository;
    private GameSessionLifecycleService lifecycleService;

    @BeforeEach
    void setUp() {
        gameSessionRepository = mock(GameSessionRepository.class);
        lifecycleService = new GameSessionLifecycleService(gameSessionRepository);
    }

    @Test
    void shouldActivateExistingSession() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = mock(GameSession.class);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        lifecycleService.activate(sessionId, "provider-session-id");

        verify(session).activate("provider-session-id");
    }

    @Test
    void shouldMarkExistingSessionFailed() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = mock(GameSession.class);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        lifecycleService.markFailed(sessionId);

        verify(session).markFailed();
    }

    @Test
    void ownerShouldCloseExistingSession() {
        UUID sessionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameSession session = mock(GameSession.class);
        when(session.getPlayerId()).thenReturn(playerId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        lifecycleService.close(sessionId, playerId);

        verify(session).close();
    }

    @Test
    void repeatedOwnerCloseDelegatesToIdempotentSessionTransition() {
        UUID sessionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        GameSession session = mock(GameSession.class);
        when(session.getPlayerId()).thenReturn(playerId);
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        lifecycleService.close(sessionId, playerId);
        lifecycleService.close(sessionId, playerId);

        verify(session, org.mockito.Mockito.times(2)).close();
    }

    @Test
    void anotherPlayerShouldNotCloseSession() {
        UUID sessionId = UUID.randomUUID();
        GameSession session = mock(GameSession.class);
        when(session.getPlayerId()).thenReturn(UUID.randomUUID());
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThrows(
                com.boki0.casino.game.exception.GameSessionAccessDeniedException.class,
                () -> lifecycleService.close(sessionId, UUID.randomUUID())
        );
        verify(session, org.mockito.Mockito.never()).close();
    }

    @Test
    void shouldRejectMissingSession() {
        UUID sessionId = UUID.randomUUID();
        when(gameSessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThrows(
                GameSessionNotFoundException.class,
                () -> lifecycleService.markFailed(sessionId)
        );
    }
}
