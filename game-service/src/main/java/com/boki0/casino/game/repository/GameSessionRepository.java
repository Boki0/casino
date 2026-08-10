package com.boki0.casino.game.repository;

import com.boki0.casino.game.domain.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameSessionRepository extends JpaRepository<GameSession, UUID> {

    Optional<GameSession> findByTokenHash(String tokenHash);

    Optional<GameSession> findByProviderSessionId(String providerSessionId);
}
