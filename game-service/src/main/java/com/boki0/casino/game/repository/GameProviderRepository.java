package com.boki0.casino.game.repository;

import com.boki0.casino.game.domain.GameProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameProviderRepository extends JpaRepository<GameProvider, UUID> {

    Optional<GameProvider> findByCode(String code);

    boolean existsByCode(String code);

    List<GameProvider> findAllByCodeIn(Collection<String> codes);

    List<GameProvider> findAllByEnabledTrueOrderByNameAsc();
}
