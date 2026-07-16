package com.boki0.casino.game.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "games",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_games_slug", columnNames = "slug"),
                @UniqueConstraint(
                        name = "uk_games_provider_game_id",
                        columnNames = {"provider_id", "provider_game_id"}
                )
        },
        indexes = {
                @Index(name = "idx_games_slug", columnList = "slug"),
                @Index(name = "idx_games_provider_id", columnList = "provider_id"),
                @Index(name = "idx_games_category", columnList = "category"),
                @Index(name = "idx_games_enabled", columnList = "enabled")
        }
)
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private GameProvider provider;

    @Column(name = "provider_game_id", nullable = false, length = 200)
    private String providerGameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 64)
    private GameCategory category;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Game() {
    }

    public Game(
            String name,
            String slug,
            GameProvider provider,
            String providerGameId,
            GameCategory category,
            String thumbnailUrl
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = normalizeSlug(Objects.requireNonNull(slug, "slug must not be null"));
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.providerGameId = Objects.requireNonNull(providerGameId, "providerGameId must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.thumbnailUrl = thumbnailUrl;
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

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase(Locale.ROOT);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = normalizeSlug(Objects.requireNonNull(slug, "slug must not be null"));
    }

    public GameProvider getProvider() {
        return provider;
    }

    public String getProviderGameId() {
        return providerGameId;
    }

    public GameCategory getCategory() {
        return category;
    }

    public void setCategory(GameCategory category) {
        this.category = Objects.requireNonNull(category, "category must not be null");
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
