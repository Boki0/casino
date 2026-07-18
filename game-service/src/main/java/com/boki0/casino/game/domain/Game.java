package com.boki0.casino.game.domain;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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

    @Column(name = "provider_available", nullable = false)
    private boolean providerAvailable = true;

    @ElementCollection
    @CollectionTable(
            name = "game_supported_currencies",
            joinColumns = @JoinColumn(name = "game_id")
    )
    @Column(name = "currency_code", nullable = false, length = 10)
    private Set<String> supportedCurrencies = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "game_supported_platforms",
            joinColumns = @JoinColumn(name = "game_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 32)
    private Set<GamePlatform> supportedPlatforms = new LinkedHashSet<>();

    @Column(name = "min_bet", nullable = false, precision = 19, scale = 4)
    private BigDecimal minBet;

    @Column(name = "max_bet", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxBet;

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
            String thumbnailUrl,
            Set<String> supportedCurrencies,
            Set<GamePlatform> supportedPlatforms,
            BigDecimal minBet,
            BigDecimal maxBet
    ) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.slug = normalizeSlug(Objects.requireNonNull(slug, "slug must not be null"));
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.providerGameId = Objects.requireNonNull(providerGameId, "providerGameId must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.thumbnailUrl = thumbnailUrl;
        this.supportedCurrencies = normalizeCurrencies(supportedCurrencies);
        this.supportedPlatforms = normalizePlatforms(supportedPlatforms);
        validateBetRange(minBet, maxBet);
        this.minBet = minBet;
        this.maxBet = maxBet;
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

    private Set<String> normalizeCurrencies(Set<String> supportedCurrencies) {
        Objects.requireNonNull(supportedCurrencies, "supportedCurrencies must not be null");

        Set<String> normalizedCurrencies = new LinkedHashSet<>();
        for (String currency : supportedCurrencies) {
            String normalizedCurrency = Objects.requireNonNull(currency, "currency code must not be null")
                    .trim()
                    .toUpperCase(Locale.ROOT);
            if (normalizedCurrency.isBlank()) {
                throw new IllegalArgumentException("currency code must not be blank");
            }
            normalizedCurrencies.add(normalizedCurrency);
        }

        return normalizedCurrencies;
    }

    private Set<GamePlatform> normalizePlatforms(Set<GamePlatform> supportedPlatforms) {
        Objects.requireNonNull(supportedPlatforms, "supportedPlatforms must not be null");

        Set<GamePlatform> normalizedPlatforms = new LinkedHashSet<>();
        for (GamePlatform platform : supportedPlatforms) {
            normalizedPlatforms.add(Objects.requireNonNull(platform, "platform must not be null"));
        }

        return normalizedPlatforms;
    }

    private void validateBetRange(BigDecimal minBet, BigDecimal maxBet) {
        Objects.requireNonNull(minBet, "minBet must not be null");
        Objects.requireNonNull(maxBet, "maxBet must not be null");

        if (minBet.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("minBet must be greater than or equal to zero");
        }
        if (maxBet.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("maxBet must be greater than or equal to zero");
        }
        if (maxBet.compareTo(minBet) < 0) {
            throw new IllegalArgumentException("maxBet must be greater than or equal to minBet");
        }
    }

    public static Game createImportedProviderGame(
            String name,
            String slug,
            GameProvider provider,
            String providerGameId,
            GameCategory category,
            String thumbnailUrl,
            boolean providerAvailable,
            Set<String> supportedCurrencies,
            Set<GamePlatform> supportedPlatforms,
            BigDecimal minBet,
            BigDecimal maxBet
    ) {
        Game game = new Game(
                name,
                slug,
                provider,
                providerGameId,
                category,
                thumbnailUrl,
                supportedCurrencies,
                supportedPlatforms,
                minBet,
                maxBet
        );
        game.enabled = false;
        game.providerAvailable = providerAvailable;
        return game;
    }

    public boolean updateProviderMetadata(
            String name,
            GameCategory category,
            String thumbnailUrl,
            boolean providerAvailable,
            Set<String> supportedCurrencies,
            Set<GamePlatform> supportedPlatforms,
            BigDecimal minBet,
            BigDecimal maxBet
    ) {
        String normalizedName = normalizeName(name);
        GameCategory normalizedCategory = Objects.requireNonNull(category, "category must not be null");
        Set<String> normalizedCurrencies = normalizeCurrencies(supportedCurrencies);
        Set<GamePlatform> normalizedPlatforms = normalizePlatforms(supportedPlatforms);
        validateBetRange(minBet, maxBet);

        boolean changed = false;
        if (!this.name.equals(normalizedName)) {
            this.name = normalizedName;
            changed = true;
        }
        if (this.category != normalizedCategory) {
            this.category = normalizedCategory;
            changed = true;
        }
        if (!Objects.equals(this.thumbnailUrl, thumbnailUrl)) {
            this.thumbnailUrl = thumbnailUrl;
            changed = true;
        }
        if (this.providerAvailable != providerAvailable) {
            this.providerAvailable = providerAvailable;
            changed = true;
        }
        if (!this.supportedCurrencies.equals(normalizedCurrencies)) {
            this.supportedCurrencies = normalizedCurrencies;
            changed = true;
        }
        if (!this.supportedPlatforms.equals(normalizedPlatforms)) {
            this.supportedPlatforms = normalizedPlatforms;
            changed = true;
        }
        if (this.minBet.compareTo(minBet) != 0) {
            this.minBet = minBet;
            changed = true;
        }
        if (this.maxBet.compareTo(maxBet) != 0) {
            this.maxBet = maxBet;
            changed = true;
        }

        return changed;
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

    public boolean isProviderAvailable() {
        return providerAvailable;
    }

    public void setProviderAvailable(boolean providerAvailable) {
        this.providerAvailable = providerAvailable;
    }

    public Set<String> getSupportedCurrencies() {
        return Collections.unmodifiableSet(supportedCurrencies);
    }

    public Set<GamePlatform> getSupportedPlatforms() {
        return Collections.unmodifiableSet(supportedPlatforms);
    }

    public BigDecimal getMinBet() {
        return minBet;
    }

    public BigDecimal getMaxBet() {
        return maxBet;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
