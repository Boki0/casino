package com.boki0.casino.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "game.session")
public class GameSessionProperties {

    private Duration ttl = Duration.ofMinutes(15);

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        Duration configuredTtl = Objects.requireNonNull(ttl, "ttl must not be null");
        if (configuredTtl.isZero() || configuredTtl.isNegative()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        this.ttl = configuredTtl;
    }
}
