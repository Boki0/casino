package com.boki0.casino.game.wallet.client;

import com.boki0.casino.game.config.WalletServiceProperties;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletBalanceClientTest {

    private static final String INTERNAL_SECRET = "internal-secret";
    private static final UUID PLAYER_ID =
            UUID.fromString("c43e2215-8c9f-4a68-a179-94fcf4274ae9");
    private static final String EXPECTED_URL =
            "http://wallet-service/internal/wallets/balance"
                    + "?playerId=" + PLAYER_ID
                    + "&currency=EUR";

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
    void getBalanceUsesInternalContractAndReturnsTypedMoneyValues() {
        server.expect(once(), requestTo(EXPECTED_URL))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Gateway-Secret", INTERNAL_SECRET))
                .andRespond(withSuccess("""
                        {
                          "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                          "currency": "EUR",
                          "cash": 1000.00,
                          "bonus": 0.00
                        }
                        """, MediaType.APPLICATION_JSON));

        WalletBalanceResponse response = client.getBalance(PLAYER_ID, "EUR");

        assertEquals(PLAYER_ID, response.playerId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("1000.00"), response.cash());
        assertEquals(new BigDecimal("0.00"), response.bonus());
        server.verify();
    }

    @Test
    void getBalanceRejectsPlayerMismatch() {
        expectSuccessResponse("""
                {
                  "playerId": "00000000-0000-0000-0000-000000000000",
                  "currency": "EUR",
                  "cash": 10.00,
                  "bonus": 0.00
                }
                """);

        assertThrows(WalletClientException.class, () -> client.getBalance(PLAYER_ID, "EUR"));
    }

    @Test
    void getBalanceRejectsCurrencyMismatch() {
        expectSuccessResponse("""
                {
                  "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                  "currency": "USD",
                  "cash": 10.00,
                  "bonus": 0.00
                }
                """);

        assertThrows(WalletClientException.class, () -> client.getBalance(PLAYER_ID, "EUR"));
    }

    @Test
    void getBalanceRejectsMissingMoneyFields() {
        expectSuccessResponse("""
                {
                  "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                  "currency": "EUR",
                  "cash": null,
                  "bonus": null
                }
                """);

        assertThrows(WalletClientException.class, () -> client.getBalance(PLAYER_ID, "EUR"));
    }

    @Test
    void getBalanceDoesNotConvertMissingWalletToZeroBalance() {
        server.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(WalletClientException.class, () -> client.getBalance(PLAYER_ID, "EUR"));
    }

    private void expectSuccessResponse(String responseBody) {
        server.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
    }
}
