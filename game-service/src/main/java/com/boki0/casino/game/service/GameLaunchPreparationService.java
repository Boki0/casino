package com.boki0.casino.game.service;

import com.boki0.casino.game.config.GameSessionProperties;
import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.domain.GameSession;
import com.boki0.casino.game.exception.GameNotFoundException;
import com.boki0.casino.game.exception.GameUnavailableException;
import com.boki0.casino.game.repository.GameRepository;
import com.boki0.casino.game.repository.GameSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

@Service
public class GameLaunchPreparationService {

    private final GameRepository gameRepository;
    private final GameSessionRepository gameSessionRepository;
    private final GameSessionTokenService tokenService;
    private final GameSessionProperties sessionProperties;

    public GameLaunchPreparationService(
            GameRepository gameRepository,
            GameSessionRepository gameSessionRepository,
            GameSessionTokenService tokenService,
            GameSessionProperties sessionProperties
    ) {
        this.gameRepository = gameRepository;
        this.gameSessionRepository = gameSessionRepository;
        this.tokenService = tokenService;
        this.sessionProperties = sessionProperties;
    }

    @Transactional
    public PreparedGameLaunch prepare(PrepareGameLaunchCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        Game game = gameRepository.findById(command.gameId())
                .orElseThrow(() -> GameNotFoundException.byId(command.gameId()));
        validatePlayable(game, command.currency());

        String rawToken = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(sessionProperties.getTtl());

        GameSession session = gameSessionRepository.save(new GameSession(
                command.playerId(),
                game,
                command.currency(),
                tokenHash,
                expiresAt
        ));

        return new PreparedGameLaunch(
                session.getId(),
                rawToken,
                game.getProvider().getCode(),
                game.getProviderGameId(),
                command.playerId(),
                command.currency(),
                expiresAt
        );
    }

    private void validatePlayable(Game game, String currency) {
        if (!game.isEnabled()) {
            throw GameUnavailableException.disabled(game.getId());
        }

        GameProvider provider = game.getProvider();
        if (!game.isProviderAvailable()
                || !provider.isEnabled()
                || !provider.isProviderAvailable()) {
            throw GameUnavailableException.providerUnavailable(game.getId());
        }

        if (!game.getSupportedCurrencies().contains(currency)) {
            throw new IllegalArgumentException("currency is not supported by the selected game");
        }
    }
}
