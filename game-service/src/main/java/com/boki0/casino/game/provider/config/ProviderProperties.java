package com.boki0.casino.game.provider.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

@ConfigurationProperties(prefix = "game.provider")
public class ProviderProperties {

    private URI baseUrl;
    private String providersPath;
    private String gamesPath;
    private String launchPath;
    private boolean allowEmptyGameSnapshot;

    public URI getBaseUrl() {
        return Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
    }

    public String getProvidersPath() {
        return Objects.requireNonNull(providersPath, "providersPath must not be null");
    }

    public void setProvidersPath(String providersPath) {
        this.providersPath = normalizePath(providersPath, "providersPath");
    }

    public String getGamesPath() {
        return Objects.requireNonNull(gamesPath, "gamesPath must not be null");
    }

    public void setGamesPath(String gamesPath) {
        this.gamesPath = normalizePath(gamesPath, "gamesPath");
    }

    public String getLaunchPath() {
        return Objects.requireNonNull(launchPath, "launchPath must not be null");
    }

    public void setLaunchPath(String launchPath) {
        this.launchPath = normalizePath(launchPath, "launchPath");
    }

    public boolean isAllowEmptyGameSnapshot() {
        return allowEmptyGameSnapshot;
    }

    public void setAllowEmptyGameSnapshot(boolean allowEmptyGameSnapshot) {
        this.allowEmptyGameSnapshot = allowEmptyGameSnapshot;
    }

    private String normalizePath(String path, String propertyName) {
        String normalizedPath = Objects.requireNonNull(path, propertyName + " must not be null").trim();
        if (normalizedPath.isBlank()) {
            throw new IllegalArgumentException(propertyName + " must not be blank");
        }
        if (!normalizedPath.startsWith("/")) {
            throw new IllegalArgumentException(propertyName + " must start with /");
        }
        return normalizedPath;
    }
}
