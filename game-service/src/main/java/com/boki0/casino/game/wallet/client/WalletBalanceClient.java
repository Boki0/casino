package com.boki0.casino.game.wallet.client;

import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import com.boki0.casino.game.wallet.exception.WalletClientException.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Objects;
import java.util.UUID;

@Component
public class WalletBalanceClient {

    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";

    private final RestClient restClient;
    private final String balancePath;
    private final String internalGatewaySecret;

    public WalletBalanceClient(
            RestClient.Builder restClientBuilder,
            WalletServiceProperties properties,
            @Value("${internal.gateway.secret}") String internalGatewaySecret
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl().toString()).build();
        this.balancePath = properties.getBalancePath();
        this.internalGatewaySecret = internalGatewaySecret;
    }

    public WalletBalanceResponse getBalance(UUID playerId, String currency) {
        Objects.requireNonNull(playerId, "playerId must not be null");
        String requiredCurrency = requireCurrency(currency);

        try {
            WalletBalanceResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(balancePath)
                            .queryParam("playerId", playerId)
                            .queryParam("currency", requiredCurrency)
                            .build())
                    .header(HEADER_INTERNAL_GATEWAY_SECRET, internalGatewaySecret)
                    .retrieve()
                    .body(WalletBalanceResponse.class);
            return validateResponse(response, playerId, requiredCurrency);
        } catch (RestClientResponseException exception) {
            throw new WalletClientException("Wallet balance request was rejected", exception);
        } catch (ResourceAccessException exception) {
            throw new WalletClientException("Wallet service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new WalletClientException("Wallet balance response could not be read", exception);
        }
    }

    private WalletBalanceResponse validateResponse(
            WalletBalanceResponse response,
            UUID expectedPlayerId,
            String expectedCurrency
    ) {
        if (response != null
                && response.currency() != null
                && !expectedCurrency.equals(response.currency())) {
            throw new WalletClientException(
                    "Wallet currency does not match requested currency",
                    Category.CURRENCY_MISMATCH
            );
        }
        if (response == null
                || !expectedPlayerId.equals(response.playerId())
                || response.currency() == null
                || response.cash() == null
                || response.bonus() == null) {
            throw new WalletClientException(
                    "Wallet service returned an invalid balance response",
                    Category.INVALID_RESPONSE
            );
        }
        return response;
    }

    private String requireCurrency(String currency) {
        String requiredCurrency = Objects.requireNonNull(
                currency,
                "currency must not be null"
        ).trim();
        if (requiredCurrency.isBlank()) {
            throw new IllegalArgumentException("currency must not be blank");
        }
        return requiredCurrency;
    }
}
