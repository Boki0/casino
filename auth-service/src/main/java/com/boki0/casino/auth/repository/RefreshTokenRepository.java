package com.boki0.casino.auth.repository;

import com.boki0.casino.auth.entity.AuthUser;
import com.boki0.casino.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void deleteByUser(AuthUser user);
}
