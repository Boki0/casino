package com.boki0.casino.game.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "game_providers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_providers_code", columnNames = "code")
        },
        indexes = {
                @Index(name = "idx_game_providers_code", columnList = "code"),
                @Index(name = "idx_game_providers_enabled", columnList = "enabled"),
                @Index(name = "idx_game_providers_provider_available", columnList = "provider_available")
        }
)
public class GameProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "code", nullable = false, unique = true, length = 100, updatable = false)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "provider_available", nullable = false)
    private boolean providerAvailable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GameProvider() {
    }

    public GameProvider(String code, String name) {
        this.code = normalizeCode(code);
        this.name = normalizeName(name);
    }

    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }

    private String normalizeCode(String code) {
        String normalizedCode = Objects.requireNonNull(code, "code must not be null")
                .trim()
                .toUpperCase(Locale.ROOT);
        if (normalizedCode.isBlank()) {
            throw new IllegalArgumentException("code must not be blank");
        }
        return normalizedCode;
    }

    private String normalizeName(String name) {
        String normalizedName = Objects.requireNonNull(name, "name must not be null").trim();
        if (normalizedName.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return normalizedName;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isProviderAvailable() {
        return providerAvailable;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void rename(String name) {
        this.name = normalizeName(name);
    }

    public boolean updateProviderMetadata(String name, boolean providerAvailable) {
        String normalizedName = normalizeName(name);
        boolean changed = false;

        if (!this.name.equals(normalizedName)) {
            this.name = normalizedName;
            changed = true;
        }
        if (this.providerAvailable != providerAvailable) {
            this.providerAvailable = providerAvailable;
            changed = true;
        }

        return changed;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
