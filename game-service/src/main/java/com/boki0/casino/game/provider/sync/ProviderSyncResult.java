package com.boki0.casino.game.provider.sync;

public record ProviderSyncResult(
        int received,
        int created,
        int updated,
        int unchanged
) {
}
