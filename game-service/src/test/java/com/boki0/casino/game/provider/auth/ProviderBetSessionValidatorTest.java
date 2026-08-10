package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GamePlatform;
import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.provider.dto.ProviderBetRequest;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.repository.GameSessionRepository;
import com.boki0.casino.game.service.GameSessionTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderBetSessionValidatorTest {

    private static final String RAW_TOKEN = "raw-provider-token";
    private static final String TOKEN_HASH = "b".repeat(64);
    private static final UUID PLAYER_ID =
            UUID.fromString("6a71d2e2-a81a-490a-adf9-d5896a20c483");

    private GameSessionTokenService tokenService;
    private GameSessionRepository gameSessionRepository;
    private ProviderBetSessionValidator validator;

    @BeforeEach
    void setUp() {
        tokenService = mock(GameSessionTokenService.class);
        gameSessionRepository = mock(GameSessionRepository.class);
        validator = new ProviderBetSessionValidator(tokenService, gameSessionRepository);
        when(tokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    }

    @Test
    void validRequestReturnsTrustedImmutableBetValues() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));

        ValidatedProviderBet bet = validator.validate(request());

        assertEquals(session.getId(), bet.localSessionId());
        assertEquals(session.getPlayerId(), bet.playerId());
        assertEquals("EUR", bet.currency());
        assertEquals("NOVA_REELS", bet.providerCode());
        assertEquals("NOVA_SEVEN", bet.gameCode());
        assertEquals("provider-session-id", bet.providerSessionId());
        assertEquals("provider-round-id", bet.roundId());
        assertEquals("provider-bet-reference", bet.reference());
        assertEquals(new BigDecimal("1.00"), bet.amount());
    }

    @Test
    void hashesRawTokenBeforeRepositoryLookup() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH))
                .thenReturn(Optional.of(activeSession(Instant.now().plusSeconds(900))));

        validator.validate(request());

        verify(tokenService).hashToken(RAW_TOKEN);
        verify(gameSessionRepository).findByTokenHash(TOKEN_HASH);
    }

    @Test
    void unknownTokenIsRejectedWithoutTokenDetails() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        ProviderAuthenticationException exception = assertRejected(request());

        assertFalse(exception.getMessage().contains(RAW_TOKEN));
        assertFalse(exception.getMessage().contains(TOKEN_HASH));
    }

    @Test
    void createdSessionIsRejected() {
        expectSession(createdSession(Instant.now().plusSeconds(900)));
        assertRejected(request());
    }

    @Test
    void failedSessionIsRejected() {
        GameSession session = createdSession(Instant.now().plusSeconds(900));
        session.markFailed();
        expectSession(session);
        assertRejected(request());
    }

    @Test
    void closedSessionIsRejected() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        session.close();
        expectSession(session);
        assertRejected(request());
    }

    @Test
    void expiredStatusSessionIsRejected() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        session.expire();
        expectSession(session);
        assertRejected(request());
    }

    @Test
    void revokedSessionIsRejected() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        session.revoke();
        expectSession(session);
        assertRejected(request());
    }

    @Test
    void timeExpiredActiveSessionIsRejectedWithoutStateMutation() {
        GameSession session = activeSession(Instant.now().minusSeconds(1));
        expectSession(session);

        assertRejected(request());

        assertEquals(com.boki0.casino.game.domain.GameSessionStatus.ACTIVE, session.getStatus());
    }

    @Test
    void providerMismatchIsRejectedWithoutCaseNormalization() {
        expectSession(activeSession(Instant.now().plusSeconds(900)));

        assertRejected(request("nova_reels", "NOVA_SEVEN", PLAYER_ID, amount("1.00"), "round", "reference"));
    }

    @Test
    void gameMismatchIsRejectedWithoutCaseNormalization() {
        expectSession(activeSession(Instant.now().plusSeconds(900)));

        assertRejected(request("NOVA_REELS", "nova_seven", PLAYER_ID, amount("1.00"), "round", "reference"));
    }

    @Test
    void userMismatchIsRejectedAndLocalPlayerRemainsSourceOfTruth() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        expectSession(session);

        assertRejected(request(
                "NOVA_REELS",
                "NOVA_SEVEN",
                UUID.randomUUID(),
                amount("1.00"),
                "round",
                "reference"
        ));

        assertEquals(PLAYER_ID, session.getPlayerId());
    }

    @Test
    void nullZeroAndNegativeAmountsAreRejectedBeforeLookup() {
        assertThrows(NullPointerException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, null, "round", "reference"
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, BigDecimal.ZERO, "round", "reference"
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, amount("-1"), "round", "reference"
        )));
    }

    @Test
    void blankRoundIdAndReferenceAreRejectedBeforeLookup() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, amount("1"), " ", "reference"
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, amount("1"), "round", " "
        )));
    }

    @Test
    void nullRequestIsRejected() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    @Test
    void validatedBetCannotExposeTokenHashRequestOrJpaEntities() {
        Set<Class<?>> componentTypes = Arrays.stream(ValidatedProviderBet.class.getRecordComponents())
                .map(component -> component.getType())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> componentNames = Arrays.stream(ValidatedProviderBet.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(componentNames.contains("token"));
        assertFalse(componentNames.contains("tokenHash"));
        assertFalse(componentTypes.contains(ProviderBetRequest.class));
        assertFalse(componentTypes.contains(GameSession.class));
        assertFalse(componentTypes.contains(Game.class));
        assertFalse(componentTypes.contains(GameProvider.class));
    }

    private ProviderAuthenticationException assertRejected(ProviderBetRequest request) {
        return assertThrows(
                ProviderAuthenticationException.class,
                () -> validator.validate(request)
        );
    }

    private void expectSession(GameSession session) {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
    }

    private ProviderBetRequest request() {
        return request(
                "NOVA_REELS",
                "NOVA_SEVEN",
                PLAYER_ID,
                amount("1.00"),
                "provider-round-id",
                "provider-bet-reference"
        );
    }

    private ProviderBetRequest request(
            String providerId,
            String gameId,
            UUID userId,
            BigDecimal amount,
            String roundId,
            String reference
    ) {
        return new ProviderBetRequest(
                userId,
                gameId,
                roundId,
                amount,
                reference,
                providerId,
                1780000000000L,
                "spin",
                "WEB",
                "en",
                RAW_TOKEN,
                "127.0.0.1"
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
                PLAYER_ID,
                game,
                "EUR",
                TOKEN_HASH,
                expiresAt
        );
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
