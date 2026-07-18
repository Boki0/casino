package com.boki0.casino.game.provider.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FullCatalogSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FullCatalogSyncService.class);

    private final ProviderSyncService providerSyncService;
    private final GameSyncService gameSyncService;

    public FullCatalogSyncService(
            ProviderSyncService providerSyncService,
            GameSyncService gameSyncService
    ) {
        this.providerSyncService = providerSyncService;
        this.gameSyncService = gameSyncService;
    }

    public FullCatalogSyncResult synchronizeCatalog() {
        LOGGER.info("Full catalog synchronization started");

        ProviderSyncResult providers = providerSyncService.synchronizeProviders();
        LOGGER.info("Provider synchronization completed for full catalog synchronization");

        GameSyncResult games = gameSyncService.synchronizeGames();
        LOGGER.info("Game synchronization completed for full catalog synchronization");

        LOGGER.info("Full catalog synchronization completed");
        return new FullCatalogSyncResult(providers, games);
    }
}
