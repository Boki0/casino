package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.domain.GameSessionStatus;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class ProviderSessionValidationService {

    private final GameSessionRepository gameSessionRepository;

    public ProviderSessionValidationService(GameSessionRepository gameSessionRepository) {
        this.gameSessionRepository = gameSessionRepository;
    }

    @Transactional(readOnly = true)
    public ValidatedProviderSession validate(
            String tokenHash,
            String providerCode,
            String gameCode,
            String providerSessionId
    ) {
        GameSession session = gameSessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(ProviderAuthenticationException::rejected);

        if (session.getStatus() != GameSessionStatus.ACTIVE
                || session.isExpiredAt(Instant.now())
                || !Objects.equals(providerCode, session.getGame().getProvider().getCode())
                || !Objects.equals(gameCode, session.getGame().getProviderGameId())
                || !Objects.equals(providerSessionId, session.getProviderSessionId())) {
            throw ProviderAuthenticationException.rejected();
        }

        return new ValidatedProviderSession(
                session.getId(),
                session.getPlayerId(),
                session.getCurrency()
        );
    }
}
