package com.boki0.casino.game.wallet.client;

import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.wallet.dto.WalletDebitRequest;
import com.boki0.casino.game.wallet.dto.WalletDebitResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletDebitClientTest {

    private static final String INTERNAL_SECRET = "internal-secret";
    private static final String DEBIT_URL = "http://wallet-service/internal/wallets/debit";
    private static final UUID PLAYER_ID =
            UUID.fromString("c43e2215-8c9f-4a68-a179-94fcf4274ae9");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("7035a82f-146a-42a0-ab95-07d867854ada");

    private MockRestServiceServer server;
    private WalletBalanceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();

        WalletServiceProperties properties = new WalletServiceProperties();
        properties.setBaseUrl(URI.create("http://wallet-service"));
        properties.setBalancePath("/internal/wallets/balance");
        properties.setDebitPath("/internal/wallets/debit");
        client = new WalletBalanceClient(builder, properties, INTERNAL_SECRET);
    }

    @Test
    void debitPostsExactContractAndReturnsTypedResponse() {
        server.expect(once(), requestTo(DEBIT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Gateway-Secret", INTERNAL_SECRET))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                          "currency": "EUR",
                          "amount": 20.00,
                          "reference": "provider-bet-1"
                        }
                        """))
                .andRespond(withSuccess(validResponse(true), MediaType.APPLICATION_JSON));

        WalletDebitResponse response = client.debit(request());

        assertEquals(TRANSACTION_ID, response.transactionId());
        assertEquals(PLAYER_ID, response.playerId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("20.00"), response.amount());
        assertEquals(new BigDecimal("980.00"), response.cash());
        assertEquals(new BigDecimal("0.00"), response.bonus());
        assertEquals("provider-bet-1", response.reference());
        assertEquals(true, response.duplicate());
        server.verify();
    }

    @Test
    void debitRejectsEmptyResponseBody() {
        expectSuccessResponse("");

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitRejectsMalformedResponseBody() {
        expectSuccessResponse("{not-json}");

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitRejectsMissingTransactionId() {
        expectSuccessResponse(validResponse(false).replace(
                "\"" + TRANSACTION_ID + "\"",
                "null"
        ));

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitRejectsPlayerMismatch() {
        expectSuccessResponse(validResponse(false).replace(
                PLAYER_ID.toString(),
                "00000000-0000-0000-0000-000000000000"
        ));

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitRejectsCurrencyMismatch() {
        expectSuccessResponse(validResponse(false).replace("\"EUR\"", "\"USD\""));

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitRejectsNumericallyDifferentAmount() {
        expectSuccessResponse(validResponse(false).replace("20.00", "21.00"));

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitAcceptsNumericallyEqualAmountWithDifferentScale() {
        expectSuccessResponse(validResponse(false).replace("20.00", "20.0"));

        WalletDebitResponse response = client.debit(request());

        assertEquals(0, response.amount().compareTo(new BigDecimal("20.00")));
    }

    @Test
    void debitRejectsReferenceMismatch() {
        expectSuccessResponse(validResponse(false).replace("provider-bet-1", "provider-bet-2"));

        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void debitMapsInvalidRequest() {
        expectStatus(HttpStatus.BAD_REQUEST, "invalid debit");

        assertCategory(WalletClientException.Category.INVALID_REQUEST);
    }

    @Test
    void debitMapsWalletNotFound() {
        expectStatus(HttpStatus.NOT_FOUND, "wallet missing");

        assertCategory(WalletClientException.Category.WALLET_NOT_FOUND);
    }

    @Test
    void debitMapsInsufficientBalanceConflict() {
        expectStatus(HttpStatus.CONFLICT, "Insufficient wallet balance");

        assertCategory(WalletClientException.Category.INSUFFICIENT_BALANCE);
    }

    @Test
    void debitMapsIdempotencyConflict() {
        expectStatus(
                HttpStatus.CONFLICT,
                "External reference has already been used for different transaction data: provider-bet-1"
        );

        assertCategory(WalletClientException.Category.IDEMPOTENCY_CONFLICT);
    }

    @Test
    void debitMapsUnprocessableInsufficientBalance() {
        expectStatus(HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient wallet balance");

        assertCategory(WalletClientException.Category.INSUFFICIENT_BALANCE);
    }

    @Test
    void debitMapsWalletServiceFailure() {
        expectStatus(HttpStatus.INTERNAL_SERVER_ERROR, "database unavailable");

        assertCategory(WalletClientException.Category.SERVICE_FAILURE);
    }

    @Test
    void debitMapsConnectionFailure() {
        server.expect(once(), requestTo(DEBIT_URL))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection timed out");
                });

        assertCategory(WalletClientException.Category.UNAVAILABLE);
    }

    @Test
    void debitRejectsInvalidClientInputBeforeCallingWalletService() {
        assertThrows(NullPointerException.class, () -> client.debit(null));
        assertThrows(NullPointerException.class, () -> client.debit(new WalletDebitRequest(
                null, "EUR", new BigDecimal("20.00"), "provider-bet-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.debit(new WalletDebitRequest(
                PLAYER_ID, " ", new BigDecimal("20.00"), "provider-bet-1"
        )));
        assertThrows(NullPointerException.class, () -> client.debit(new WalletDebitRequest(
                PLAYER_ID, "EUR", null, "provider-bet-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.debit(new WalletDebitRequest(
                PLAYER_ID, "EUR", BigDecimal.ZERO, "provider-bet-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.debit(new WalletDebitRequest(
                PLAYER_ID, "EUR", new BigDecimal("-1.00"), "provider-bet-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.debit(new WalletDebitRequest(
                PLAYER_ID, "EUR", new BigDecimal("20.00"), " "
        )));
    }

    private WalletDebitRequest request() {
        return new WalletDebitRequest(
                PLAYER_ID,
                "EUR",
                new BigDecimal("20.00"),
                "provider-bet-1"
        );
    }

    private String validResponse(boolean duplicate) {
        return """
                {
                  "transactionId": "%s",
                  "playerId": "%s",
                  "currency": "EUR",
                  "amount": 20.00,
                  "cash": 980.00,
                  "bonus": 0.00,
                  "reference": "provider-bet-1",
                  "duplicate": %s
                }
                """.formatted(TRANSACTION_ID, PLAYER_ID, duplicate);
    }

    private void expectSuccessResponse(String responseBody) {
        server.expect(once(), requestTo(DEBIT_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void expectStatus(HttpStatus status, String message) {
        server.expect(once(), requestTo(DEBIT_URL))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "status": %d,
                                  "message": "%s"
                                }
                                """.formatted(status.value(), message)));
    }

    private void assertCategory(WalletClientException.Category expectedCategory) {
        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> client.debit(request())
        );
        assertEquals(expectedCategory, exception.getCategory());
    }
}
