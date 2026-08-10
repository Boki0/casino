package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.exception.GlobalExceptionHandler;
import com.boki0.casino.wallet.exception.InsufficientWalletBalanceException;
import com.boki0.casino.wallet.exception.WalletNotFoundException;
import com.boki0.casino.wallet.exception.WalletTransactionIdempotencyConflictException;
import com.boki0.casino.wallet.security.GatewayInternalAuthFilter;
import com.boki0.casino.wallet.service.WalletDebitCommand;
import com.boki0.casino.wallet.service.WalletDebitResult;
import com.boki0.casino.wallet.service.WalletDebitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalWalletDebitControllerTest {

    private static final String INTERNAL_SECRET = "local-gateway-secret-change-me";
    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";
    private static final UUID PLAYER_ID =
            UUID.fromString("c43e2215-8c9f-4a68-a179-94fcf4274ae9");

    private final FakeWalletDebitService walletDebitService = new FakeWalletDebitService();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new InternalWalletDebitController(walletDebitService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new GatewayInternalAuthFilter(INTERNAL_SECRET))
            .build();

    @Test
    void debitWithoutInternalSecretReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/wallets/debit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validRequestDelegatesToServiceAndMapsFreshResult() throws Exception {
        UUID transactionId = UUID.randomUUID();
        walletDebitService.result = result(transactionId, false);

        mockMvc.perform(validDebitRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.playerId").value(PLAYER_ID.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amount").value(20.00))
                .andExpect(jsonPath("$.cash").value(980.00))
                .andExpect(jsonPath("$.bonus").value(0))
                .andExpect(jsonPath("$.reference").value("manual-bet-test-001"))
                .andExpect(jsonPath("$.duplicate").value(false));

        assertEquals(PLAYER_ID, walletDebitService.command.playerId());
        assertEquals("EUR", walletDebitService.command.currency());
        assertEquals(new BigDecimal("20.00"), walletDebitService.command.amount());
        assertEquals("manual-bet-test-001", walletDebitService.command.externalReference());
    }

    @Test
    void identicalDuplicateReturnsHttp200AndOriginalStoredResult() throws Exception {
        UUID transactionId = UUID.randomUUID();
        walletDebitService.result = result(transactionId, true);

        mockMvc.perform(validDebitRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.cash").value(980.00))
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"currency\":\"EUR\",\"amount\":20.00,\"reference\":\"bet-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"\",\"amount\":20.00,\"reference\":\"bet-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"reference\":\"bet-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"amount\":0,\"reference\":\"bet-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"amount\":-1,\"reference\":\"bet-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"amount\":20.00,\"reference\":\"\"}"
    })
    void invalidRequestReturnsBadRequest(String requestBody) throws Exception {
        mockMvc.perform(post("/internal/wallets/debit")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void walletNotFoundReturnsNotFound() throws Exception {
        walletDebitService.exception = WalletNotFoundException.forPlayerAndCurrency(PLAYER_ID, "EUR");

        mockMvc.perform(validDebitRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void insufficientBalanceReturnsConflict() throws Exception {
        walletDebitService.exception = new InsufficientWalletBalanceException();

        mockMvc.perform(validDebitRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void idempotencyConflictReturnsConflict() throws Exception {
        walletDebitService.exception =
                new WalletTransactionIdempotencyConflictException("manual-bet-test-001");

        mockMvc.perform(validDebitRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validDebitRequest() {
        return post("/internal/wallets/debit")
                .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest());
    }

    private String validRequest() {
        return """
                {
                  "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                  "currency": "EUR",
                  "amount": 20.00,
                  "reference": "manual-bet-test-001"
                }
                """;
    }

    private WalletDebitResult result(UUID transactionId, boolean duplicate) {
        return new WalletDebitResult(
                transactionId,
                PLAYER_ID,
                "EUR",
                new BigDecimal("20.00"),
                new BigDecimal("980.00"),
                "manual-bet-test-001",
                duplicate
        );
    }

    private static class FakeWalletDebitService extends WalletDebitService {
        private WalletDebitCommand command;
        private WalletDebitResult result;
        private RuntimeException exception;

        FakeWalletDebitService() {
            super(null, null, null);
        }

        @Override
        public WalletDebitResult debit(WalletDebitCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
