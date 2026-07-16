package com.boki0.casino.game.service;

import com.boki0.casino.game.api.dto.GameResponse;
import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.exception.GameNotFoundException;
import com.boki0.casino.game.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class GameCatalogService {

    private final GameRepository gameRepository;

    public GameCatalogService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public List<GameResponse> getEnabledGames() {
        return gameRepository.findAllByEnabledTrueAndProviderAvailableTrueOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public GameResponse getEnabledGameById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Game id must not be null");
        }

        Game game = gameRepository.findByIdAndEnabledTrueAndProviderAvailableTrue(id)
                .orElseThrow(() -> GameNotFoundException.byId(id));

        return toResponse(game);
    }

    public GameResponse getEnabledGameBySlug(String slug) {
        if (slug == null) {
            throw new IllegalArgumentException("Game slug must not be null");
        }

        String normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
        Game game = gameRepository.findBySlugAndEnabledTrueAndProviderAvailableTrue(normalizedSlug)
                .orElseThrow(() -> GameNotFoundException.bySlug(normalizedSlug));

        return toResponse(game);
    }

    private GameResponse toResponse(Game game) {
        return new GameResponse(
                game.getId(),
                game.getName(),
                game.getSlug(),
                game.getProvider().getCode(),
                game.getCategory(),
                game.getThumbnailUrl(),
                copySet(game.getSupportedCurrencies()),
                copySet(game.getSupportedPlatforms()),
                game.getMinBet(),
                game.getMaxBet()
        );
    }

    private <T> Set<T> copySet(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
