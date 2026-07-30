package com.boki0.casino.game.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

@ConfigurationProperties(prefix = "game.wallet")
public class WalletServiceProperties {

    private URI baseUrl;
    private String balancePath;

    public URI getBaseUrl() {
        return Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    }

    public String getBalancePath() {
        return Objects.requireNonNull(balancePath, "balancePath must not be null");
    }

    public void setBalancePath(String balancePath) {
        String normalizedPath = Objects.requireNonNull(
                balancePath,
                "balancePath must not be null"
        ).trim();
        if (normalizedPath.isBlank() || !normalizedPath.startsWith("/")) {
            throw new IllegalArgumentException("balancePath must start with /");
        }
        this.balancePath = normalizedPath;
    }
}
