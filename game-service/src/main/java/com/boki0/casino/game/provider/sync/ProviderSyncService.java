package com.boki0.casino.game.provider.sync;

import com.boki0.casino.game.provider.client.ProviderCatalogClient;
import com.boki0.casino.game.provider.dto.ProviderResponse;
import com.boki0.casino.game.provider.exception.ProviderSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProviderSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderSyncService.class);

    private final ProviderCatalogClient providerCatalogClient;
    private final ProviderSyncPersistenceService providerSyncPersistenceService;

    public ProviderSyncService(
            ProviderCatalogClient providerCatalogClient,
            ProviderSyncPersistenceService providerSyncPersistenceService
    ) {
        this.providerCatalogClient = providerCatalogClient;
        this.providerSyncPersistenceService = providerSyncPersistenceService;
    }

    public ProviderSyncResult synchronizeProviders() {
        List<ProviderResponse> providers = providerCatalogClient.fetchProviders();
        try {
            List<ValidatedProvider> validatedProviders = validateProviders(providers);
            ProviderSyncResult result = providerSyncPersistenceService.synchronizeProviders(validatedProviders);

            LOGGER.info(
                    "Provider synchronization completed: received={}, created={}, updated={}, unchanged={}",
                    result.received(),
                    result.created(),
                    result.updated(),
                    result.unchanged()
            );

            return result;
        } catch (ProviderSyncException exception) {
            LOGGER.warn("Provider synchronization failed: {}", exception.getMessage());
            throw exception;
        }
    }

    private List<ValidatedProvider> validateProviders(List<ProviderResponse> providers) {
        if (providers == null) {
            throw ProviderSyncException.invalidProviderData("Provider response must not be null");
        }

        Set<String> providerCodes = new LinkedHashSet<>();
        List<ValidatedProvider> validatedProviders = new ArrayList<>();

        for (ProviderResponse provider : providers) {
            if (provider == null) {
                throw ProviderSyncException.invalidProviderData("Provider record must not be null");
            }

            String providerCode = normalizeProviderCode(provider.providerCode());
            String name = normalizeName(provider.name());

            if (!providerCodes.add(providerCode)) {
                throw ProviderSyncException.invalidProviderData("Duplicate provider code: " + providerCode);
            }

            validatedProviders.add(new ValidatedProvider(providerCode, name, provider.active()));
        }

        return List.copyOf(validatedProviders);
    }

    private String normalizeProviderCode(String providerCode) {
        if (providerCode == null) {
            throw ProviderSyncException.invalidProviderData("providerCode must not be null");
        }

        String normalizedProviderCode = providerCode.trim().toUpperCase(Locale.ROOT);
        if (normalizedProviderCode.isBlank()) {
            throw ProviderSyncException.invalidProviderData("providerCode must not be blank");
        }
        return normalizedProviderCode;
    }

    private String normalizeName(String name) {
        if (name == null) {
            throw ProviderSyncException.invalidProviderData("name must not be null");
        }

        String normalizedName = name.trim();
        if (normalizedName.isBlank()) {
            throw ProviderSyncException.invalidProviderData("name must not be blank");
        }
        return normalizedName;
    }

    public record ValidatedProvider(
            String providerCode,
            String name,
            boolean providerAvailable
    ) {
    }
}
