package com.boki0.casino.game.provider.client;

import com.boki0.casino.game.provider.config.ProviderProperties;
import com.boki0.casino.game.provider.dto.ProviderGameResponse;
import com.boki0.casino.game.provider.dto.ProviderLaunchRequest;
import com.boki0.casino.game.provider.dto.ProviderLaunchResponse;
import com.boki0.casino.game.provider.dto.ProviderResponse;
import com.boki0.casino.game.provider.exception.ProviderCatalogException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Objects;

@Component
public class ProviderCatalogClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderCatalogClient.class);
    private static final ParameterizedTypeReference<List<ProviderResponse>> PROVIDERS_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<ProviderGameResponse>> GAMES_RESPONSE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String providersPath;
    private final String gamesPath;
    private final String launchPath;

    public ProviderCatalogClient(
            RestClient.Builder restClientBuilder,
            ProviderProperties providerProperties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(providerProperties.getBaseUrl().toString())
                .build();
        this.providersPath = providerProperties.getProvidersPath();
        this.gamesPath = providerProperties.getGamesPath();
        this.launchPath = providerProperties.getLaunchPath();
    }

    public List<ProviderResponse> fetchProviders() {
        return fetchList(
                providersPath,
                PROVIDERS_RESPONSE_TYPE,
                "provider list",
                "providers",
                "Provider list request failed"
        );
    }

    public List<ProviderGameResponse> fetchGames() {
        return fetchList(
                gamesPath,
                GAMES_RESPONSE_TYPE,
                "game catalog",
                "games",
                "Provider game catalog request failed"
        );
    }

    public ProviderLaunchResponse launchGame(ProviderLaunchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        LOGGER.info(
                "Launching provider game {} for provider {}",
                request.gameCode(),
                request.providerCode()
        );

        try {
            ProviderLaunchResponse response = restClient.post()
                    .uri(launchPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ProviderLaunchResponse.class);

            validateLaunchResponse(response);
            LOGGER.info("Provider game launch created session {}", response.sessionId());
            return response;
        } catch (ProviderCatalogException exception) {
            LOGGER.warn("Provider launch failed: {}", exception.getMessage());
            throw exception;
        } catch (RestClientResponseException exception) {
            throw providerCatalogException(providerLaunchHttpError(exception), exception);
        } catch (ResourceAccessException exception) {
            throw providerCatalogException("Provider launch communication failed", exception);
        } catch (RestClientException exception) {
            throw providerCatalogException("Provider launch response could not be read", exception);
        } catch (RuntimeException exception) {
            throw providerCatalogException("Provider launch response could not be read", exception);
        }
    }

    private void validateLaunchResponse(ProviderLaunchResponse response) {
        if (response == null) {
            throw new ProviderCatalogException("Provider launch response body was empty");
        }
        if (response.sessionId() == null || response.sessionId().isBlank()) {
            throw new ProviderCatalogException("Provider launch response is missing sessionId");
        }
        if (response.launchUrl() == null || response.launchUrl().isBlank()) {
            throw new ProviderCatalogException("Provider launch response is missing launchUrl");
        }
    }

    private String providerLaunchHttpError(RestClientResponseException exception) {
        int status = exception.getStatusCode().value();
        return switch (status) {
            case 400 -> "Provider rejected launch request with HTTP status 400";
            case 404 -> "Provider launch resource was not found with HTTP status 404";
            case 409 -> "Provider rejected launch due to a state conflict with HTTP status 409";
            default -> exception.getStatusCode().is5xxServerError()
                    ? "Provider launch service was unavailable with HTTP status " + status
                    : "Provider launch failed with HTTP status " + status;
        };
    }

    private <T> List<T> fetchList(
            String path,
            ParameterizedTypeReference<List<T>> responseType,
            String requestName,
            String countName,
            String failureMessage
    ) {
        LOGGER.debug("Fetching provider {}", requestName);

        try {
            List<T> response = restClient.get()
                    .uri(path)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new ProviderCatalogException(failureMessage + ": response body was empty");
            }

            LOGGER.info("Fetched {} provider {}", response.size(), countName);
            return List.copyOf(response);
        } catch (ProviderCatalogException exception) {
            LOGGER.warn("{}: {}", failureMessage, exception.getMessage());
            throw exception;
        } catch (RestClientResponseException exception) {
            throw providerCatalogException(failureMessage + " with HTTP status "
                    + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw providerCatalogException(failureMessage, exception);
        } catch (RuntimeException exception) {
            throw providerCatalogException(failureMessage + ": response could not be read", exception);
        }
    }

    private ProviderCatalogException providerCatalogException(String message, Throwable cause) {
        LOGGER.warn("Provider catalog fetch failed: {}", message);
        return new ProviderCatalogException(message, cause);
    }
}
