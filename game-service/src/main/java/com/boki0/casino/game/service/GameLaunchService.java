package com.boki0.casino.game.service;

import com.boki0.casino.game.provider.client.ProviderCatalogClient;
import com.boki0.casino.game.provider.dto.ProviderLaunchRequest;
import com.boki0.casino.game.provider.dto.ProviderLaunchResponse;
import com.boki0.casino.game.provider.exception.ProviderCatalogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class GameLaunchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameLaunchService.class);
    private static final String REAL_MODE = "REAL";

    private final GameLaunchPreparationService preparationService;
    private final ProviderCatalogClient providerClient;
    private final GameSessionLifecycleService lifecycleService;

    public GameLaunchService(
            GameLaunchPreparationService preparationService,
            ProviderCatalogClient providerClient,
            GameSessionLifecycleService lifecycleService
    ) {
        this.preparationService = preparationService;
        this.providerClient = providerClient;
        this.lifecycleService = lifecycleService;
    }

    public GameLaunchResult launch(PrepareGameLaunchCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        PreparedGameLaunch prepared = preparationService.prepare(command);
        ProviderLaunchRequest providerRequest = toProviderRequest(prepared);

        try {
            ProviderLaunchResponse providerResponse = providerClient.launchGame(providerRequest);
            lifecycleService.activate(prepared.localSessionId(), providerResponse.sessionId());
            return new GameLaunchResult(prepared.localSessionId(), providerResponse.launchUrl());
        } catch (ProviderCatalogException providerException) {
            markFailedPreservingProviderException(prepared, providerException);
            throw providerException;
        }
    }

    private ProviderLaunchRequest toProviderRequest(PreparedGameLaunch prepared) {
        return new ProviderLaunchRequest(
                prepared.providerCode(),
                prepared.gameCode(),
                prepared.playerId().toString(),
                prepared.currency(),
                prepared.rawToken(),
                REAL_MODE
        );
    }

    private void markFailedPreservingProviderException(
            PreparedGameLaunch prepared,
            ProviderCatalogException providerException
    ) {
        try {
            lifecycleService.markFailed(prepared.localSessionId());
        } catch (RuntimeException lifecycleException) {
            providerException.addSuppressed(lifecycleException);
            LOGGER.error(
                    "Could not mark local game session {} as FAILED after provider launch failure",
                    prepared.localSessionId(),
                    lifecycleException
            );
        }
    }
}
