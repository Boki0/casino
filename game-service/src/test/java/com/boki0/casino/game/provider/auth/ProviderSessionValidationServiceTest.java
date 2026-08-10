package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GamePlatform;
import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderSessionValidationServiceTest {

    private static final String TOKEN_HASH = "b".repeat(64);

    private GameSessionRepository gameSessionRepository;
    private ProviderSessionValidationService validationService;

    @BeforeEach
    void setUp() {
        gameSessionRepository = mock(GameSessionRepository.class);
        validationService = new ProviderSessionValidationService(gameSessionRepository);
    }

    @Test
    void shouldReturnImmutableValuesForMatchingActiveSession() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        ValidatedProviderSession validated = validationService.validate(
                TOKEN_HASH,
                "NOVA_REELS",
                "NOVA_SEVEN",
                "provider-session-id"
        );

        assertEquals(session.getId(), validated.localSessionId());
        assertEquals(session.getPlayerId(), validated.playerId());
        assertEquals("EUR", validated.currency());
        verify(gameSessionRepository).findByTokenHash(TOKEN_HASH);
    }

    @Test
    void shouldRejectUnknownTokenHash() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectInactiveSession() {
        GameSession session = createdSession(Instant.now().plusSeconds(900));
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectFailedSession() {
        GameSession session = createdSession(Instant.now().plusSeconds(900));
        session.markFailed();
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectClosedSession() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        session.close();
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectSessionWithExpiredStatus() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        session.expire();
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectRevokedSession() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        session.revoke();
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectExpiredSession() {
        GameSession session = activeSession(Instant.now().minusSeconds(1));
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectProviderMismatch() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(Instant.now().plusSeconds(900))));

        assertRejected(TOKEN_HASH, "ATLAS_GAMING", "NOVA_SEVEN", "provider-session-id");
    }

    @Test
    void shouldRejectGameMismatch() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(Instant.now().plusSeconds(900))));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_STAR_VAULT", "provider-session-id");
    }

    @Test
    void shouldRejectProviderSessionMismatch() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(Instant.now().plusSeconds(900))));

        assertRejected(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "other-provider-session");
    }

    private void assertRejected(
            String tokenHash,
            String providerCode,
            String gameCode,
            String providerSessionId
    ) {
        assertThrows(
                ProviderAuthenticationException.class,
                () -> validationService.validate(
                        tokenHash,
                        providerCode,
                        gameCode,
                        providerSessionId
                )
        );
    }

    private GameSession activeSession(Instant expiresAt) {
        GameSession session = createdSession(expiresAt);
        session.activate("provider-session-id");
        return session;
    }

    private GameSession createdSession(Instant expiresAt) {
        GameProvider provider = new GameProvider("NOVA_REELS", "Nova Reels");
        Game game = Game.createImportedProviderGame(
                "Nova Seven",
                "nova-reels-nova-seven",
                provider,
                "NOVA_SEVEN",
                GameCategory.SLOTS,
                "/images/nova-seven.jpg",
                true,
                Set.of("EUR"),
                Set.of(GamePlatform.DESKTOP),
                BigDecimal.ONE,
                BigDecimal.TEN
        );
        GameSession session = new GameSession(
                UUID.randomUUID(),
                game,
                "EUR",
                TOKEN_HASH,
                expiresAt
        );
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }
}
