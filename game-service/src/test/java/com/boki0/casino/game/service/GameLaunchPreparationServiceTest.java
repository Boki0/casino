package com.boki0.casino.game.service;

import com.boki0.casino.game.config.GameSessionProperties;
import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GamePlatform;
import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.domain.GameSessionStatus;
import com.boki0.casino.game.exception.GameNotFoundException;
import com.boki0.casino.game.exception.GameUnavailableException;
import com.boki0.casino.game.repository.GameRepository;
import com.boki0.casino.game.repository.GameSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameLaunchPreparationServiceTest {

    private static final String RAW_TOKEN = "raw-session-token";
    private static final String TOKEN_HASH = "a".repeat(64);

    private GameRepository gameRepository;
    private GameSessionRepository gameSessionRepository;
    private GameSessionTokenService tokenService;
    private GameLaunchPreparationService preparationService;

    @BeforeEach
    void setUp() {
        gameRepository = mock(GameRepository.class);
        gameSessionRepository = mock(GameSessionRepository.class);
        tokenService = mock(GameSessionTokenService.class);

        GameSessionProperties sessionProperties = new GameSessionProperties();
        sessionProperties.setTtl(Duration.ofMinutes(15));
        preparationService = new GameLaunchPreparationService(
                gameRepository,
                gameSessionRepository,
                tokenService,
                sessionProperties
        );
    }

    @Test
    void shouldPrepareAndPersistCreatedSession() {
        Game game = createPlayableGame();
        UUID playerId = UUID.randomUUID();
        UUID localSessionId = UUID.randomUUID();
        Instant beforePreparation = Instant.now();

        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));
        when(tokenService.generateToken()).thenReturn(RAW_TOKEN);
        when(tokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(gameSessionRepository.save(any(GameSession.class))).thenAnswer(invocation -> {
            GameSession session = invocation.getArgument(0);
            ReflectionTestUtils.setField(session, "id", localSessionId);
            return session;
        });

        PreparedGameLaunch prepared = preparationService.prepare(
                new PrepareGameLaunchCommand(game.getId(), playerId, " eur ")
        );

        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(gameSessionRepository).save(sessionCaptor.capture());
        GameSession savedSession = sessionCaptor.getValue();

        assertEquals(localSessionId, prepared.localSessionId());
        assertEquals(RAW_TOKEN, prepared.rawToken());
        assertEquals("NOVA_REELS", prepared.providerCode());
        assertEquals("NOVA_SEVEN", prepared.gameCode());
        assertEquals(playerId, prepared.playerId());
        assertEquals("EUR", prepared.currency());
        assertTrue(prepared.expiresAt().isAfter(beforePreparation));

        assertEquals(GameSessionStatus.CREATED, savedSession.getStatus());
        assertEquals(TOKEN_HASH, savedSession.getTokenHash());
        assertFalse(savedSession.getTokenHash().equals(RAW_TOKEN));
        assertEquals("EUR", savedSession.getCurrency());
        assertNull(savedSession.getProviderSessionId());
        assertNull(savedSession.getActivatedAt());
        assertNull(savedSession.getClosedAt());
        assertEquals(prepared.expiresAt(), savedSession.getExpiresAt());
    }

    @Test
    void shouldRejectMissingGame() {
        UUID gameId = UUID.randomUUID();
        when(gameRepository.findById(gameId)).thenReturn(Optional.empty());

        assertThrows(
                GameNotFoundException.class,
                () -> preparationService.prepare(commandFor(gameId))
        );

        verify(gameSessionRepository, never()).save(any());
        verify(tokenService, never()).generateToken();
    }

    @Test
    void shouldRejectDisabledGame() {
        Game game = createPlayableGame();
        game.setEnabled(false);
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

        assertThrows(
                GameUnavailableException.class,
                () -> preparationService.prepare(commandFor(game.getId()))
        );

        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectProviderUnavailableGame() {
        Game game = createPlayableGame();
        game.setProviderAvailable(false);
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

        assertThrows(
                GameUnavailableException.class,
                () -> preparationService.prepare(commandFor(game.getId()))
        );

        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void shouldRejectUnsupportedCurrency() {
        Game game = createPlayableGame();
        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

        assertThrows(
                IllegalArgumentException.class,
                () -> preparationService.prepare(
                        new PrepareGameLaunchCommand(game.getId(), UUID.randomUUID(), "USD")
                )
        );

        verify(gameSessionRepository, never()).save(any());
    }

    @Test
    void shouldNormalizeCurrencyAndValidateCommandFields() {
        UUID gameId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        PrepareGameLaunchCommand command = new PrepareGameLaunchCommand(gameId, playerId, " eur ");

        assertEquals("EUR", command.currency());
        assertThrows(NullPointerException.class, () -> new PrepareGameLaunchCommand(null, playerId, "EUR"));
        assertThrows(NullPointerException.class, () -> new PrepareGameLaunchCommand(gameId, null, "EUR"));
        assertThrows(NullPointerException.class, () -> new PrepareGameLaunchCommand(gameId, playerId, null));
        assertThrows(IllegalArgumentException.class, () -> new PrepareGameLaunchCommand(gameId, playerId, "   "));
    }

    @Test
    void shouldRedactRawTokenFromPreparedLaunchString() {
        PreparedGameLaunch prepared = new PreparedGameLaunch(
                UUID.randomUUID(),
                RAW_TOKEN,
                "NOVA_REELS",
                "NOVA_SEVEN",
                UUID.randomUUID(),
                "EUR",
                Instant.now().plusSeconds(900)
        );

        assertFalse(prepared.toString().contains(RAW_TOKEN));
        assertTrue(prepared.toString().contains("rawToken=<redacted>"));
    }

    private PrepareGameLaunchCommand commandFor(UUID gameId) {
        return new PrepareGameLaunchCommand(gameId, UUID.randomUUID(), "EUR");
    }

    private Game createPlayableGame() {
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
        ReflectionTestUtils.setField(game, "id", UUID.randomUUID());
        return game;
    }
}
