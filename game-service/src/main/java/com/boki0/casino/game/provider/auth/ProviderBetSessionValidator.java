package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.domain.GameSessionStatus;
import com.boki0.casino.game.provider.dto.ProviderBetRequest;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.repository.GameSessionRepository;
import com.boki0.casino.game.service.GameSessionTokenService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Service
public class ProviderBetSessionValidator {

    private final GameSessionTokenService tokenService;
    private final GameSessionRepository gameSessionRepository;

    public ProviderBetSessionValidator(
            GameSessionTokenService tokenService,
            GameSessionRepository gameSessionRepository
    ) {
        this.tokenService = tokenService;
        this.gameSessionRepository = gameSessionRepository;
    }

    @Transactional(readOnly = true)
    public ValidatedProviderBet validate(ProviderBetRequest request) {
        ProviderBetRequest requiredRequest = validateRequest(request);
        String tokenHash = tokenService.hashToken(requiredRequest.token());
        GameSession session = gameSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(ProviderAuthenticationException::rejected);

        validateSession(session, requiredRequest);

        Game game = session.getGame();
        return new ValidatedProviderBet(
                session.getId(),
                session.getPlayerId(),
                session.getCurrency(),
                game.getProvider().getCode(),
                game.getProviderGameId(),
                session.getProviderSessionId(),
                requiredRequest.roundId(),
                requiredRequest.reference(),
                requiredRequest.amount()
        );
    }

    private ProviderBetRequest validateRequest(ProviderBetRequest request) {
        ProviderBetRequest requiredRequest = Objects.requireNonNull(
                request,
                "request must not be null"
        );
        requireNonBlank(requiredRequest.token(), "token");
        requireNonBlank(requiredRequest.providerId(), "providerId");
        requireNonBlank(requiredRequest.gameId(), "gameId");
        requireNonBlank(requiredRequest.roundId(), "roundId");
        requireNonBlank(requiredRequest.reference(), "reference");

        BigDecimal amount = Objects.requireNonNull(
                requiredRequest.amount(),
                "amount must not be null"
        );
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        return requiredRequest;
    }

    private void validateSession(GameSession session, ProviderBetRequest request) {
        Game game = session.getGame();
        if (session.getStatus() != GameSessionStatus.ACTIVE
                || session.isExpiredAt(Instant.now())
                || !Objects.equals(request.providerId(), game.getProvider().getCode())
                || !Objects.equals(request.gameId(), game.getProviderGameId())
                || !Objects.equals(request.userId(), session.getPlayerId())) {
            throw ProviderAuthenticationException.rejected();
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }
}
