package com.boki0.casino.game.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
        name = "game_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_sessions_token_hash", columnNames = "token_hash"),
                @UniqueConstraint(
                        name = "uk_game_sessions_provider_session_id",
                        columnNames = "provider_session_id"
                )
        },
        indexes = {
                @Index(name = "idx_game_sessions_player_id", columnList = "player_id"),
                @Index(name = "idx_game_sessions_game_id", columnList = "game_id"),
                @Index(name = "idx_game_sessions_status", columnList = "status")
        }
)
public class GameSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false, updatable = false)
    private Game game;

    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String tokenHash;

    @Column(name = "provider_session_id", unique = true, length = 200)
    private String providerSessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GameSessionStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    protected GameSession() {
    }

    public GameSession(UUID playerId, Game game, String currency, String tokenHash, Instant expiresAt) {
        this.playerId = Objects.requireNonNull(playerId, "playerId must not be null");
        this.game = Objects.requireNonNull(game, "game must not be null");
        this.currency = normalizeCurrency(currency);
        this.tokenHash = requireNonBlank(tokenHash, "tokenHash");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.status = GameSessionStatus.CREATED;
        this.createdAt = Instant.now();
    }

    private String normalizeCurrency(String currency) {
        String normalizedCurrency = requireNonBlank(currency, "currency").toUpperCase(Locale.ROOT);
        if (normalizedCurrency.length() > 3) {
            throw new IllegalArgumentException("currency must not exceed 3 characters");
        }
        return normalizedCurrency;
    }

    private String requireNonBlank(String value, String fieldName) {
        String normalizedValue = Objects.requireNonNull(value, fieldName + " must not be null").trim();
        if (normalizedValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return normalizedValue;
    }

    public void activate(String providerSessionId) {
        requireStatus(GameSessionStatus.CREATED, "activate");

        String normalizedProviderSessionId = requireNonBlank(providerSessionId, "providerSessionId");
        if (this.providerSessionId != null && !this.providerSessionId.equals(normalizedProviderSessionId)) {
            throw InvalidGameSessionStateException.forOperation(
                    status,
                    "activate",
                    "provider session ID is already assigned"
            );
        }

        this.providerSessionId = normalizedProviderSessionId;
        this.status = GameSessionStatus.ACTIVE;
        this.activatedAt = Instant.now();
    }

    public void markFailed() {
        requireStatus(GameSessionStatus.CREATED, "mark failed");
        enterTerminalState(GameSessionStatus.FAILED);
    }

    public void close() {
        if (status == GameSessionStatus.CLOSED) {
            return;
        }

        requireStatus(GameSessionStatus.ACTIVE, "close");
        enterTerminalState(GameSessionStatus.CLOSED);
    }

    public void expire() {
        if (status == GameSessionStatus.EXPIRED) {
            return;
        }

        requireStatus(GameSessionStatus.ACTIVE, "expire");
        enterTerminalState(GameSessionStatus.EXPIRED);
    }

    public void revoke() {
        if (status == GameSessionStatus.REVOKED) {
            return;
        }
        if (status != GameSessionStatus.CREATED && status != GameSessionStatus.ACTIVE) {
            throw InvalidGameSessionStateException.forOperation(status, "revoke");
        }

        enterTerminalState(GameSessionStatus.REVOKED);
    }

    public boolean isActive() {
        return status == GameSessionStatus.ACTIVE;
    }

    public boolean isTerminal() {
        return status == GameSessionStatus.FAILED
                || status == GameSessionStatus.CLOSED
                || status == GameSessionStatus.EXPIRED
                || status == GameSessionStatus.REVOKED;
    }

    public boolean isExpiredAt(Instant now) {
        Objects.requireNonNull(now, "now must not be null");
        return !now.isBefore(expiresAt);
    }

    private void requireStatus(GameSessionStatus requiredStatus, String operation) {
        if (status != requiredStatus) {
            throw InvalidGameSessionStateException.forOperation(status, operation);
        }
    }

    private void enterTerminalState(GameSessionStatus terminalStatus) {
        status = terminalStatus;
        closedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public Game getGame() {
        return game;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getProviderSessionId() {
        return providerSessionId;
    }

    public GameSessionStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}
