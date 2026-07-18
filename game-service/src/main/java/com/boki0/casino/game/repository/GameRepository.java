package com.boki0.casino.game.repository;

import com.boki0.casino.game.domain.Game;
import com.boki0.casino.game.domain.GameCategory;
import com.boki0.casino.game.domain.GameProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    Optional<Game> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Optional<Game> findByProviderAndProviderGameId(GameProvider provider, String providerGameId);

    @EntityGraph(attributePaths = {"provider", "supportedCurrencies", "supportedPlatforms"})
    List<Game> findAllByProvider_IdIn(Collection<UUID> providerIds);

    Optional<Game> findByIdAndEnabledTrueAndProviderAvailableTrue(UUID id);

    Optional<Game> findBySlugAndEnabledTrueAndProviderAvailableTrue(String slug);

    List<Game> findAllByEnabledTrueAndProviderAvailableTrueOrderByNameAsc();

    List<Game> findAllByEnabledTrueOrderByNameAsc();

    List<Game> findAllByCategoryAndEnabledTrueOrderByNameAsc(GameCategory category);

    List<Game> findAllByProviderAndEnabledTrueOrderByNameAsc(GameProvider provider);
}
