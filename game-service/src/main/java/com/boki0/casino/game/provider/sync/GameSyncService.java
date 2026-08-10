package com.boki0.casino.game.provider.sync;

import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GamePlatform;
import com.boki0.casino.game.provider.client.ProviderCatalogClient;
import com.boki0.casino.game.provider.config.ProviderProperties;
import com.boki0.casino.game.provider.dto.ProviderGameResponse;
import com.boki0.casino.game.provider.exception.GameSyncException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class GameSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GameSyncService.class);

    private final ProviderCatalogClient providerCatalogClient;
    private final GameSyncPersistenceService gameSyncPersistenceService;
    private final ProviderProperties providerProperties;

    public GameSyncService(
            ProviderCatalogClient providerCatalogClient,
            GameSyncPersistenceService gameSyncPersistenceService,
            ProviderProperties providerProperties
    ) {
        this.providerCatalogClient = providerCatalogClient;
        this.gameSyncPersistenceService = gameSyncPersistenceService;
        this.providerProperties = providerProperties;
    }

    public GameSyncResult synchronizeGames() {
        List<ProviderGameResponse> games = providerCatalogClient.fetchGames();
        try {
            List<ValidatedGame> validatedGames = validateGames(games);
            rejectUnsafeEmptySnapshot(validatedGames);
            GameSyncResult result = gameSyncPersistenceService.synchronizeGames(validatedGames);

            LOGGER.info(
                    "Game synchronization completed: received={}, created={}, updated={}, unchanged={}, markedUnavailable={}",
                    result.received(),
                    result.created(),
                    result.updated(),
                    result.unchanged(),
                    result.markedUnavailable()
            );

            return result;
        } catch (GameSyncException exception) {
            LOGGER.warn("Game synchronization failed: {}", exception.getMessage());
            throw exception;
        }
    }

    private void rejectUnsafeEmptySnapshot(List<ValidatedGame> games) {
        if (games.isEmpty() && !providerProperties.isAllowEmptyGameSnapshot()) {
            throw GameSyncException.invalidGameData(
                    "Provider game snapshot is empty; set GAME_PROVIDER_ALLOW_EMPTY_GAME_SNAPSHOT=true to allow reconciliation"
            );
        }
    }

    private List<ValidatedGame> validateGames(List<ProviderGameResponse> games) {
        if (games == null) {
            throw GameSyncException.invalidGameData("Provider game response must not be null");
        }

        Set<GameIdentity> identities = new LinkedHashSet<>();
        List<ValidatedGame> validatedGames = new ArrayList<>();

        for (ProviderGameResponse game : games) {
            if (game == null) {
                throw GameSyncException.invalidGameData("Provider game record must not be null");
            }

            String providerCode = normalizeRequiredUppercase(game.providerCode(), "providerCode");
            String gameCode = normalizeRequiredUppercase(game.gameCode(), "gameCode");
            GameIdentity identity = new GameIdentity(providerCode, gameCode);
            if (!identities.add(identity)) {
                throw GameSyncException.invalidGameData(
                        "Duplicate provider game identity: " + providerCode + " + " + gameCode
                );
            }

            BigDecimal minBet = validateBetValue(game.minBet(), "minBet");
            BigDecimal maxBet = validateBetRange(minBet, game.maxBet());

            validatedGames.add(new ValidatedGame(
                    identity,
                    normalizeRequiredName(game.name()),
                    mapCategory(game.category()),
                    game.active(),
                    immutableSet(normalizeCurrencies(game.supportedCurrencies())),
                    immutableSet(mapPlatforms(game.supportedPlatforms())),
                    minBet,
                    maxBet,
                    game.imgUrl()
            ));
        }

        return List.copyOf(validatedGames);
    }

    private String normalizeRequiredUppercase(String value, String fieldName) {
        if (value == null) {
            throw GameSyncException.invalidGameData(fieldName + " must not be null");
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        if (normalizedValue.isBlank()) {
            throw GameSyncException.invalidGameData(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    private String normalizeRequiredName(String name) {
        if (name == null) {
            throw GameSyncException.invalidGameData("name must not be null");
        }

        String normalizedName = name.trim();
        if (normalizedName.isBlank()) {
            throw GameSyncException.invalidGameData("name must not be blank");
        }
        return normalizedName;
    }

    private GameCategory mapCategory(String category) {
        String normalizedCategory = normalizeRequiredUppercase(category, "category");
        try {
            return GameCategory.valueOf(normalizedCategory);
        } catch (IllegalArgumentException exception) {
            throw GameSyncException.invalidGameData("Unsupported game category: " + normalizedCategory, exception);
        }
    }

    private Set<String> normalizeCurrencies(Set<String> currencies) {
        if (currencies == null) {
            throw GameSyncException.invalidGameData("supportedCurrencies must not be null");
        }

        Set<String> normalizedCurrencies = new LinkedHashSet<>();
        for (String currency : currencies) {
            normalizedCurrencies.add(normalizeRequiredUppercase(currency, "currency"));
        }
        return normalizedCurrencies;
    }

    private Set<GamePlatform> mapPlatforms(Set<String> platforms) {
        if (platforms == null) {
            throw GameSyncException.invalidGameData("supportedPlatforms must not be null");
        }

        Set<GamePlatform> mappedPlatforms = new LinkedHashSet<>();
        for (String platform : platforms) {
            String normalizedPlatform = normalizeRequiredUppercase(platform, "platform");
            try {
                mappedPlatforms.add(GamePlatform.valueOf(normalizedPlatform));
            } catch (IllegalArgumentException exception) {
                throw GameSyncException.invalidGameData("Unsupported game platform: " + normalizedPlatform, exception);
            }
        }
        return mappedPlatforms;
    }

    private <T> Set<T> immutableSet(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private BigDecimal validateBetValue(BigDecimal betValue, String fieldName) {
        if (betValue == null) {
            throw GameSyncException.invalidGameData(fieldName + " must not be null");
        }
        if (betValue.compareTo(BigDecimal.ZERO) < 0) {
            throw GameSyncException.invalidGameData(fieldName + " must be greater than or equal to zero");
        }
        return betValue;
    }

    private BigDecimal validateBetRange(BigDecimal minBet, BigDecimal maxBet) {
        BigDecimal validatedMinBet = validateBetValue(minBet, "minBet");
        BigDecimal validatedMaxBet = validateBetValue(maxBet, "maxBet");
        if (validatedMaxBet.compareTo(validatedMinBet) < 0) {
            throw GameSyncException.invalidGameData("maxBet must be greater than or equal to minBet");
        }
        return validatedMaxBet;
    }

    public record ValidatedGame(
            GameIdentity identity,
            String name,
            GameCategory category,
            boolean providerAvailable,
            Set<String> supportedCurrencies,
            Set<GamePlatform> supportedPlatforms,
            BigDecimal minBet,
            BigDecimal maxBet,
            String thumbnailUrl
    ) {
    }
}
