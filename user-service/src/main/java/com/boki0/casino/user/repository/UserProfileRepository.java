package com.boki0.casino.user.repository;

import com.boki0.casino.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {

    Optional<UserProfile> findByAuthUserId(UUID authUserId);

    Optional<UserProfile> findByUsername(String username);

    Optional<UserProfile> findByEmail(String email);

    boolean existsByAuthUserId(UUID authUserId);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
