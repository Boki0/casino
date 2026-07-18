package com.boki0.casino.game.provider.sync;

public record FullCatalogSyncResult(
        ProviderSyncResult providers,
        GameSyncResult games
) {
}
