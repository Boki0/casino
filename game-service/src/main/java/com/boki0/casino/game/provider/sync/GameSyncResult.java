package com.boki0.casino.game.provider.sync;

public record GameSyncResult(
        int received,
        int created,
        int updated,
        int unchanged
) {
}
