package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.client.ProviderCatalogClient;
import com.boki0.casino.game.provider.dto.ProviderGameResponse;
import com.boki0.casino.game.provider.dto.ProviderResponse;
import com.boki0.casino.game.provider.sync.ProviderSyncResult;
import com.boki0.casino.game.provider.sync.ProviderSyncService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal/provider-catalog")
public class InternalProviderCatalogController {

    private final ProviderCatalogClient providerCatalogClient;
    private final ProviderSyncService providerSyncService;

    public InternalProviderCatalogController(
            ProviderCatalogClient providerCatalogClient,
            ProviderSyncService providerSyncService
    ) {
        this.providerCatalogClient = providerCatalogClient;
        this.providerSyncService = providerSyncService;
    }

    // Temporary development endpoint for manually testing provider catalog reads.
    @GetMapping("/providers")
    public List<ProviderResponse> getProviders() {
        return providerCatalogClient.fetchProviders();
    }

    // Temporary development endpoint for manually testing provider catalog reads.
    @GetMapping("/games")
    public List<ProviderGameResponse> getProviderGames() {
        return providerCatalogClient.fetchGames();
    }

    @PostMapping("/sync/providers")
    public ProviderSyncResult synchronizeProviders() {
        return providerSyncService.synchronizeProviders();
    }
}
