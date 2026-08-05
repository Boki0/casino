package com.boki0.casino.game.wallet.client;

import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.wallet.dto.WalletCreditRequest;
import com.boki0.casino.game.wallet.dto.WalletCreditResponse;
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

class WalletCreditClientTest {

    private static final String INTERNAL_SECRET = "internal-secret";
    private static final String CREDIT_URL = "http://wallet-service/internal/wallets/credit";
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
        properties.setCreditPath("/internal/wallets/credit");
        client = new WalletBalanceClient(builder, properties, INTERNAL_SECRET);
    }

    @Test
    void creditPostsExactContractAndReturnsTypedResponse() {
        server.expect(once(), requestTo(CREDIT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-Internal-Gateway-Secret", INTERNAL_SECRET))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                          "currency": "EUR",
                          "amount": 50.00,
                          "reference": "provider-result-1"
                        }
                        """))
                .andRespond(withSuccess(validResponse(true), MediaType.APPLICATION_JSON));

        WalletCreditResponse response = client.credit(request());

        assertEquals(TRANSACTION_ID, response.transactionId());
        assertEquals(PLAYER_ID, response.playerId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("50.00"), response.amount());
        assertEquals(new BigDecimal("980.00"), response.balanceBefore());
        assertEquals(new BigDecimal("1030.00"), response.cash());
        assertEquals(new BigDecimal("0.00"), response.bonus());
        assertEquals("provider-result-1", response.reference());
        assertEquals(true, response.duplicate());
        server.verify();
    }

    @Test
    void creditRejectsEmptyResponseBody() {
        expectSuccessResponse("");
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void creditRejectsMalformedResponseBody() {
        expectSuccessResponse("{not-json}");
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void creditRejectsMissingRequiredResponseFields() {
        assertInvalidResponse(validResponse(false).replace("\"" + TRANSACTION_ID + "\"", "null"));
        assertInvalidResponse(validResponse(false).replace("\"balanceBefore\": 980.00", "\"balanceBefore\": null"));
        assertInvalidResponse(validResponse(false).replace("\"cash\": 1030.00", "\"cash\": null"));
        assertInvalidResponse(validResponse(false).replace("\"bonus\": 0.00", "\"bonus\": null"));
    }

    @Test
    void creditRejectsPlayerMismatch() {
        expectSuccessResponse(validResponse(false).replace(
                PLAYER_ID.toString(), "00000000-0000-0000-0000-000000000000"
        ));
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void creditRejectsCurrencyMismatch() {
        expectSuccessResponse(validResponse(false).replace("\"EUR\"", "\"USD\""));
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void creditRejectsNumericallyDifferentAmount() {
        expectSuccessResponse(validResponse(false).replace("50.00", "51.00"));
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void creditAcceptsNumericallyEqualAmountWithDifferentScale() {
        expectSuccessResponse(validResponse(false).replace("50.00", "50.0"));

        WalletCreditResponse response = client.credit(request());

        assertEquals(0, response.amount().compareTo(new BigDecimal("50.00")));
    }

    @Test
    void creditRejectsReferenceMismatch() {
        expectSuccessResponse(validResponse(false).replace("provider-result-1", "provider-result-2"));
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
    }

    @Test
    void creditMapsKnownHttpErrors() {
        expectStatus(HttpStatus.BAD_REQUEST);
        assertCategory(WalletClientException.Category.INVALID_REQUEST);
        server.reset();

        expectStatus(HttpStatus.NOT_FOUND);
        assertCategory(WalletClientException.Category.WALLET_NOT_FOUND);
        server.reset();

        expectStatus(HttpStatus.CONFLICT);
        assertCategory(WalletClientException.Category.IDEMPOTENCY_CONFLICT);
        server.reset();

        expectStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertCategory(WalletClientException.Category.SERVICE_FAILURE);
    }

    @Test
    void creditMapsConnectionOrTimeoutFailure() {
        server.expect(once(), requestTo(CREDIT_URL))
                .andRespond(request -> {
                    throw new ResourceAccessException("connection timed out");
                });

        assertCategory(WalletClientException.Category.UNAVAILABLE);
    }

    @Test
    void creditRejectsInvalidClientInputBeforeCallingWalletService() {
        assertThrows(NullPointerException.class, () -> client.credit(null));
        assertThrows(NullPointerException.class, () -> client.credit(new WalletCreditRequest(
                null, "EUR", new BigDecimal("50.00"), "provider-result-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.credit(new WalletCreditRequest(
                PLAYER_ID, " ", new BigDecimal("50.00"), "provider-result-1"
        )));
        assertThrows(NullPointerException.class, () -> client.credit(new WalletCreditRequest(
                PLAYER_ID, "EUR", null, "provider-result-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.credit(new WalletCreditRequest(
                PLAYER_ID, "EUR", BigDecimal.ZERO, "provider-result-1"
        )));
        assertThrows(IllegalArgumentException.class, () -> client.credit(new WalletCreditRequest(
                PLAYER_ID, "EUR", new BigDecimal("-1.00"), "provider-result-1"
        )));
        assertThrows(NullPointerException.class, () -> client.credit(new WalletCreditRequest(
                PLAYER_ID, "EUR", new BigDecimal("50.00"), null
        )));
        assertThrows(IllegalArgumentException.class, () -> client.credit(new WalletCreditRequest(
                PLAYER_ID, "EUR", new BigDecimal("50.00"), " "
        )));
    }

    private WalletCreditRequest request() {
        return new WalletCreditRequest(
                PLAYER_ID, "EUR", new BigDecimal("50.00"), "provider-result-1"
        );
    }

    private String validResponse(boolean duplicate) {
        return """
                {
                  "transactionId": "%s",
                  "playerId": "%s",
                  "currency": "EUR",
                  "amount": 50.00,
                  "balanceBefore": 980.00,
                  "cash": 1030.00,
                  "bonus": 0.00,
                  "reference": "provider-result-1",
                  "duplicate": %s
                }
                """.formatted(TRANSACTION_ID, PLAYER_ID, duplicate);
    }

    private void assertInvalidResponse(String responseBody) {
        expectSuccessResponse(responseBody);
        assertCategory(WalletClientException.Category.INVALID_RESPONSE);
        server.reset();
    }

    private void expectSuccessResponse(String responseBody) {
        server.expect(once(), requestTo(CREDIT_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }

    private void expectStatus(HttpStatus status) {
        server.expect(once(), requestTo(CREDIT_URL))
                .andRespond(withStatus(status)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"status\":" + status.value() + ",\"message\":\"safe error\"}"));
    }

    private void assertCategory(WalletClientException.Category expectedCategory) {
        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> client.credit(request())
        );
        assertEquals(expectedCategory, exception.getCategory());
    }
}
