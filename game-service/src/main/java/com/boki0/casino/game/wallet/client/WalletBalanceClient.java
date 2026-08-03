package com.boki0.casino.game.wallet.client;

import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.dto.WalletDebitRequest;
import com.boki0.casino.game.wallet.dto.WalletDebitResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import com.boki0.casino.game.wallet.exception.WalletClientException.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Component
public class WalletBalanceClient {

    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";

    private final RestClient restClient;
    private final String balancePath;
    private final String debitPath;
    private final String internalGatewaySecret;

    public WalletBalanceClient(
            RestClient.Builder restClientBuilder,
            WalletServiceProperties properties,
            @Value("${internal.gateway.secret}") String internalGatewaySecret
    ) {
        this.restClient = restClientBuilder.baseUrl(properties.getBaseUrl().toString()).build();
        this.balancePath = properties.getBalancePath();
        this.debitPath = properties.getDebitPath();
        this.internalGatewaySecret = internalGatewaySecret;
    }

    public WalletDebitResponse debit(WalletDebitRequest request) {
        WalletDebitRequest requiredRequest = validateDebitRequest(request);

        try {
            WalletDebitResponse response = restClient.post()
                    .uri(debitPath)
                    .header(HEADER_INTERNAL_GATEWAY_SECRET, internalGatewaySecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requiredRequest)
                    .retrieve()
                    .body(WalletDebitResponse.class);
            return validateDebitResponse(response, requiredRequest);
        } catch (RestClientResponseException exception) {
            throw mapDebitHttpError(exception);
        } catch (ResourceAccessException exception) {
            throw new WalletClientException("Wallet service is unavailable", exception);
        } catch (RestClientException exception) {
            throw new WalletClientException(
                    "Wallet debit response could not be read",
                    exception,
                    Category.INVALID_RESPONSE
            );
        }
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

    private WalletDebitRequest validateDebitRequest(WalletDebitRequest request) {
        WalletDebitRequest requiredRequest = Objects.requireNonNull(
                request,
                "request must not be null"
        );
        Objects.requireNonNull(requiredRequest.playerId(), "playerId must not be null");
        requireCurrency(requiredRequest.currency());
        BigDecimal amount = Objects.requireNonNull(
                requiredRequest.amount(),
                "amount must not be null"
        );
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        String reference = Objects.requireNonNull(
                requiredRequest.reference(),
                "reference must not be null"
        );
        if (reference.isBlank()) {
            throw new IllegalArgumentException("reference must not be blank");
        }
        return requiredRequest;
    }

    private WalletDebitResponse validateDebitResponse(
            WalletDebitResponse response,
            WalletDebitRequest request
    ) {
        if (response == null
                || response.transactionId() == null
                || response.playerId() == null
                || response.currency() == null
                || response.amount() == null
                || response.cash() == null
                || response.bonus() == null
                || response.reference() == null) {
            throw invalidDebitResponse();
        }
        if (!request.playerId().equals(response.playerId())
                || !request.currency().equals(response.currency())
                || request.amount().compareTo(response.amount()) != 0
                || !request.reference().equals(response.reference())) {
            throw invalidDebitResponse();
        }
        return response;
    }

    private WalletClientException invalidDebitResponse() {
        return new WalletClientException(
                "Wallet service returned an invalid debit response",
                Category.INVALID_RESPONSE
        );
    }

    private WalletClientException mapDebitHttpError(RestClientResponseException exception) {
        return switch (exception.getStatusCode().value()) {
            case 400 -> new WalletClientException(
                    "Wallet debit request was rejected as invalid",
                    exception,
                    Category.INVALID_REQUEST
            );
            case 404 -> new WalletClientException(
                    "Wallet was not found",
                    exception,
                    Category.WALLET_NOT_FOUND
            );
            case 409 -> mapDebitConflict(exception);
            case 422 -> new WalletClientException(
                    "Wallet balance is insufficient",
                    exception,
                    Category.INSUFFICIENT_BALANCE
            );
            default -> new WalletClientException(
                    "Wallet service failed to process debit",
                    exception,
                    exception.getStatusCode().is5xxServerError()
                            ? Category.SERVICE_FAILURE
                            : Category.UNAVAILABLE
            );
        };
    }

    private WalletClientException mapDebitConflict(RestClientResponseException exception) {
        WalletErrorResponse errorResponse = readErrorResponse(exception);
        if (errorResponse != null && "Insufficient wallet balance".equals(errorResponse.message())) {
            return new WalletClientException(
                    "Wallet balance is insufficient",
                    exception,
                    Category.INSUFFICIENT_BALANCE
            );
        }
        if (errorResponse != null
                && errorResponse.message() != null
                && errorResponse.message().startsWith(
                        "External reference has already been used for different transaction data:"
                )) {
            return new WalletClientException(
                    "Wallet debit reference conflicts with an existing transaction",
                    exception,
                    Category.IDEMPOTENCY_CONFLICT
            );
        }
        return new WalletClientException(
                "Wallet service returned an unrecognized conflict response",
                exception,
                Category.INVALID_RESPONSE
        );
    }

    private WalletErrorResponse readErrorResponse(RestClientResponseException exception) {
        try {
            return exception.getResponseBodyAs(WalletErrorResponse.class);
        } catch (RestClientException | IllegalStateException ignored) {
            return null;
        }
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

    private record WalletErrorResponse(String message) {
    }
}
