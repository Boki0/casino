package com.boki0.casino.game.provider.sync;

import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameProvider;
import com.boki0.casino.game.provider.exception.GameSyncException;
import com.boki0.casino.game.repository.GameProviderRepository;
import com.boki0.casino.game.repository.GameRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GameSyncPersistenceService {

    private final GameProviderRepository gameProviderRepository;
    private final GameRepository gameRepository;

    public GameSyncPersistenceService(
            GameProviderRepository gameProviderRepository,
            GameRepository gameRepository
    ) {
        this.gameProviderRepository = gameProviderRepository;
        this.gameRepository = gameRepository;
    }

    @Transactional
    public GameSyncResult synchronizeGames(List<GameSyncService.ValidatedGame> games) {
        try {
            return synchronizeGamesTransactional(games);
        } catch (DataAccessException exception) {
            throw GameSyncException.persistenceFailure("Game synchronization persistence failed", exception);
        }
    }

    private GameSyncResult synchronizeGamesTransactional(List<GameSyncService.ValidatedGame> games) {
        if (games.isEmpty()) {
            return new GameSyncResult(0, 0, 0, 0);
        }

        Map<String, GameProvider> providersByCode = loadReferencedProviders(games);
        Map<GameIdentity, Game> existingGames = loadExistingGames(providersByCode.values());

        List<Game> gamesToSave = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int unchanged = 0;

        for (GameSyncService.ValidatedGame game : games) {
            GameProvider provider = providersByCode.get(game.identity().providerCode());
            Game existingGame = existingGames.get(game.identity());

            if (existingGame == null) {
                gamesToSave.add(createGame(game, provider));
                created++;
                continue;
            }

            if (existingGame.updateProviderMetadata(
                    game.name(),
                    game.category(),
                    game.thumbnailUrl(),
                    game.providerAvailable(),
                    game.supportedCurrencies(),
                    game.supportedPlatforms(),
                    game.minBet(),
                    game.maxBet()
            )) {
                gamesToSave.add(existingGame);
                updated++;
            } else {
                unchanged++;
            }
        }

        if (!gamesToSave.isEmpty()) {
            gameRepository.saveAll(gamesToSave);
            gameRepository.flush();
        }

        return new GameSyncResult(games.size(), created, updated, unchanged);
    }

    private Map<String, GameProvider> loadReferencedProviders(List<GameSyncService.ValidatedGame> games) {
        Set<String> providerCodes = games.stream()
                .map(game -> game.identity().providerCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, GameProvider> providersByCode = gameProviderRepository.findAllByCodeIn(providerCodes)
                .stream()
                .collect(Collectors.toMap(GameProvider::getCode, Function.identity()));

        for (String providerCode : providerCodes) {
            if (!providersByCode.containsKey(providerCode)) {
                throw GameSyncException.missingProvider("Missing local provider for code: " + providerCode);
            }
        }

        return providersByCode;
    }

    private Map<GameIdentity, Game> loadExistingGames(Collection<GameProvider> providers) {
        List<UUID> providerIds = providers.stream()
                .map(GameProvider::getId)
                .toList();

        return gameRepository.findAllByProvider_IdIn(providerIds)
                .stream()
                .collect(Collectors.toMap(
                        game -> new GameIdentity(game.getProvider().getCode(), game.getProviderGameId()),
                        Function.identity()
                ));
    }

    private Game createGame(GameSyncService.ValidatedGame game, GameProvider provider) {
        return Game.createImportedProviderGame(
                game.name(),
                generateSlug(game.identity()),
                provider,
                game.identity().gameCode(),
                game.category(),
                game.thumbnailUrl(),
                game.providerAvailable(),
                game.supportedCurrencies(),
                game.supportedPlatforms(),
                game.minBet(),
                game.maxBet()
        );
    }

    private String generateSlug(GameIdentity identity) {
        String slug = (identity.providerCode() + "-" + identity.gameCode())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (slug.isBlank()) {
            throw GameSyncException.invalidGameData(
                    "Game slug could not be generated for identity: "
                            + identity.providerCode()
                            + " + "
                            + identity.gameCode()
            );
        }

        return slug;
    }
}
