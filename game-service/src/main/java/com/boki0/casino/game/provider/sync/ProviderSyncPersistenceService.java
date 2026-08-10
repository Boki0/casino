package com.boki0.casino.game.provider.sync;

import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.provider.exception.ProviderSyncException;
import com.boki0.casino.game.repository.GameProviderRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProviderSyncPersistenceService {

    private final GameProviderRepository gameProviderRepository;

    public ProviderSyncPersistenceService(GameProviderRepository gameProviderRepository) {
        this.gameProviderRepository = gameProviderRepository;
    }

    @Transactional
    public ProviderSyncResult synchronizeProviders(List<ProviderSyncService.ValidatedProvider> providers) {
        try {
            return synchronizeProvidersTransactional(providers);
        } catch (DataAccessException exception) {
            throw ProviderSyncException.persistenceFailure("Provider synchronization persistence failed", exception);
        }
    }

    private ProviderSyncResult synchronizeProvidersTransactional(List<ProviderSyncService.ValidatedProvider> providers) {
        if (providers.isEmpty()) {
            return new ProviderSyncResult(0, 0, 0, 0);
        }

        List<String> providerCodes = providers.stream()
                .map(ProviderSyncService.ValidatedProvider::providerCode)
                .toList();
        Map<String, GameProvider> existingProviders = gameProviderRepository.findAllByCodeIn(providerCodes)
                .stream()
                .collect(Collectors.toMap(GameProvider::getCode, Function.identity()));

        List<GameProvider> providersToSave = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (ProviderSyncService.ValidatedProvider provider : providers) {
            GameProvider existingProvider = existingProviders.get(provider.providerCode());
            if (existingProvider == null) {
                GameProvider newProvider = new GameProvider(provider.providerCode(), provider.name());
                newProvider.updateProviderMetadata(provider.name(), provider.providerAvailable());
                providersToSave.add(newProvider);
                created++;
                continue;
            }

            if (existingProvider.updateProviderMetadata(provider.name(), provider.providerAvailable())) {
                providersToSave.add(existingProvider);
                updated++;
            } else {
                unchanged++;
            }
        }

        if (!providersToSave.isEmpty()) {
            gameProviderRepository.saveAll(providersToSave);
            gameProviderRepository.flush();
        }

        return new ProviderSyncResult(providers.size(), created, updated, unchanged);
    }
}
