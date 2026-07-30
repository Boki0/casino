package com.boki0.casino.game.wallet.client;

import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
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

    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";
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

    public WalletBalanceResponse getBalance(UUID playerId) {
        Objects.requireNonNull(playerId, "playerId must not be null");

        try {
            WalletBalanceResponse response = restClient.get()
                    .uri(balancePath)
                    .header(HEADER_AUTH_USER_ID, playerId.toString())
                    .header(HEADER_INTERNAL_GATEWAY_SECRET, internalGatewaySecret)
                    .retrieve()
                    .body(WalletBalanceResponse.class);
            return validateResponse(response, playerId);
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
            UUID expectedPlayerId
    ) {
        if (response == null
                || !expectedPlayerId.equals(response.authUserId())
                || response.balance() == null
                || response.currency() == null
                || response.currency().isBlank()) {
            throw new WalletClientException("Wallet service returned an invalid balance response");
        }
        return response;
    }
}
