package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GamePlatform;
import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.domain.GameSessionStatus;
import com.boki0.casino.game.provider.dto.ProviderResultRequest;
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

class ProviderResultSessionValidatorTest {

    private static final String RAW_TOKEN = "raw-provider-token";
    private static final String TOKEN_HASH = "b".repeat(64);
    private static final UUID PLAYER_ID =
            UUID.fromString("6a71d2e2-a81a-490a-adf9-d5896a20c483");

    private GameSessionTokenService tokenService;
    private GameSessionRepository gameSessionRepository;
    private ProviderResultSessionValidator validator;

    @BeforeEach
    void setUp() {
        tokenService = mock(GameSessionTokenService.class);
        gameSessionRepository = mock(GameSessionRepository.class);
        validator = new ProviderResultSessionValidator(tokenService, gameSessionRepository);
        when(tokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
    }

    @Test
    void validActiveResultReturnsTrustedImmutableValues() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        expectSession(session);

        ValidatedProviderResult result = validator.validate(request(new BigDecimal("50.00")));

        assertEquals(session.getId(), result.localSessionId());
        assertEquals(session.getPlayerId(), result.playerId());
        assertEquals("EUR", result.currency());
        assertEquals("NOVA_REELS", result.providerCode());
        assertEquals("NOVA_SEVEN", result.gameCode());
        assertEquals("provider-session-id", result.providerSessionId());
        assertEquals("provider-round-id", result.roundId());
        assertEquals("provider-result-reference", result.reference());
        assertEquals(new BigDecimal("50.00"), result.amount());
    }

    @Test
    void hashesRawTokenBeforeRepositoryLookup() {
        expectSession(activeSession(Instant.now().plusSeconds(900)));

        validator.validate(request(BigDecimal.ZERO));

        verify(tokenService).hashToken(RAW_TOKEN);
        verify(gameSessionRepository).findByTokenHash(TOKEN_HASH);
    }

    @Test
    void unknownTokenIsRejectedWithoutTokenDetails() {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.empty());

        ProviderAuthenticationException exception = assertRejected(request(BigDecimal.ZERO));

        assertFalse(exception.getMessage().contains(RAW_TOKEN));
        assertFalse(exception.getMessage().contains(TOKEN_HASH));
    }

    @Test
    void createdFailedExpiredAndRevokedSessionsAreRejected() {
        GameSession created = createdSession(Instant.now().plusSeconds(900));
        expectSession(created);
        assertRejected(request(BigDecimal.ZERO));

        GameSession failed = createdSession(Instant.now().plusSeconds(900));
        failed.markFailed();
        expectSession(failed);
        assertRejected(request(BigDecimal.ZERO));

        GameSession expired = activeSession(Instant.now().plusSeconds(900));
        expired.expire();
        expectSession(expired);
        assertRejected(request(BigDecimal.ZERO));

        GameSession revoked = activeSession(Instant.now().plusSeconds(900));
        revoked.revoke();
        expectSession(revoked);
        assertRejected(request(BigDecimal.ZERO));

    }

    @Test
    void closedSessionCanSettleAnAlreadyAcceptedResultUntilExpiry() {
        GameSession closed = activeSession(Instant.now().plusSeconds(900));
        closed.close();
        expectSession(closed);

        assertEquals(
                new BigDecimal("50.00"),
                validator.validate(request(new BigDecimal("50.00"))).amount()
        );
    }

    @Test
    void timeExpiredActiveSessionIsRejectedWithoutStateMutation() {
        GameSession session = activeSession(Instant.now().minusSeconds(1));
        expectSession(session);

        assertRejected(request(BigDecimal.ZERO));

        assertEquals(GameSessionStatus.ACTIVE, session.getStatus());
    }

    @Test
    void providerAndGameMismatchesAreRejectedWithoutCaseNormalization() {
        expectSession(activeSession(Instant.now().plusSeconds(900)));
        assertRejected(request("nova_reels", "NOVA_SEVEN", PLAYER_ID, BigDecimal.ZERO, "round", "reference"));

        expectSession(activeSession(Instant.now().plusSeconds(900)));
        assertRejected(request("NOVA_REELS", "nova_seven", PLAYER_ID, BigDecimal.ZERO, "round", "reference"));
    }

    @Test
    void userMismatchIsRejectedAndLocalPlayerIsSourceOfTruth() {
        GameSession session = activeSession(Instant.now().plusSeconds(900));
        expectSession(session);

        assertRejected(request(
                "NOVA_REELS", "NOVA_SEVEN", UUID.randomUUID(), BigDecimal.ZERO, "round", "reference"
        ));

        assertEquals(PLAYER_ID, session.getPlayerId());
    }

    @Test
    void zeroAndPositiveAmountsAreAccepted() {
        expectSession(activeSession(Instant.now().plusSeconds(900)));
        assertEquals(0, validator.validate(request(BigDecimal.ZERO)).amount().compareTo(BigDecimal.ZERO));

        expectSession(activeSession(Instant.now().plusSeconds(900)));
        assertEquals(0, validator.validate(request(new BigDecimal("50.00"))).amount()
                .compareTo(new BigDecimal("50.00")));
    }

    @Test
    void nullAndNegativeAmountsAreRejectedBeforeLookup() {
        assertThrows(NullPointerException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, null, "round", "reference"
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, new BigDecimal("-0.01"), "round", "reference"
        )));
    }

    @Test
    void blankRoundIdAndReferenceAreRejectedBeforeLookup() {
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, BigDecimal.ZERO, " ", "reference"
        )));
        assertThrows(IllegalArgumentException.class, () -> validator.validate(request(
                "NOVA_REELS", "NOVA_SEVEN", PLAYER_ID, BigDecimal.ZERO, "round", " "
        )));
    }

    @Test
    void nullRequestIsRejected() {
        assertThrows(NullPointerException.class, () -> validator.validate(null));
    }

    @Test
    void resultContractHasNoProviderSessionIdSoTokenProviderAndGameBindSession() {
        Set<String> requestFields = Arrays.stream(ProviderResultRequest.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(requestFields.contains("sessionId"));
    }

    @Test
    void validatedResultCannotExposeTokenHashRequestOrJpaEntities() {
        Set<Class<?>> componentTypes = Arrays.stream(ValidatedProviderResult.class.getRecordComponents())
                .map(component -> component.getType())
                .collect(java.util.stream.Collectors.toSet());
        Set<String> componentNames = Arrays.stream(ValidatedProviderResult.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(componentNames.contains("token"));
        assertFalse(componentNames.contains("tokenHash"));
        assertFalse(componentTypes.contains(ProviderResultRequest.class));
        assertFalse(componentTypes.contains(GameSession.class));
        assertFalse(componentTypes.contains(Game.class));
        assertFalse(componentTypes.contains(GameProvider.class));
    }

    private ProviderAuthenticationException assertRejected(ProviderResultRequest request) {
        return assertThrows(ProviderAuthenticationException.class, () -> validator.validate(request));
    }

    private void expectSession(GameSession session) {
        when(gameSessionRepository.findByTokenHash(TOKEN_HASH)).thenReturn(Optional.of(session));
    }

    private ProviderResultRequest request(BigDecimal amount) {
        return request(
                "NOVA_REELS",
                "NOVA_SEVEN",
                PLAYER_ID,
                amount,
                "provider-round-id",
                "provider-result-reference"
        );
    }

    private ProviderResultRequest request(
            String providerId,
            String gameId,
            UUID userId,
            BigDecimal amount,
            String roundId,
            String reference
    ) {
        return new ProviderResultRequest(
                userId,
                gameId,
                roundId,
                amount,
                reference,
                providerId,
                1780000001000L,
                "spin",
                "WEB",
                RAW_TOKEN,
                null,
                null,
                null,
                null,
                null
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
        GameSession session = new GameSession(PLAYER_ID, game, "EUR", TOKEN_HASH, expiresAt);
        ReflectionTestUtils.setField(session, "id", UUID.randomUUID());
        return session;
    }
}
