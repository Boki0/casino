package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.exception.GlobalExceptionHandler;
import com.boki0.casino.wallet.exception.WalletNotFoundException;
import com.boki0.casino.wallet.exception.WalletTransactionIdempotencyConflictException;
import com.boki0.casino.wallet.security.GatewayInternalAuthFilter;
import com.boki0.casino.wallet.service.WalletCreditCommand;
import com.boki0.casino.wallet.service.WalletCreditResult;
import com.boki0.casino.wallet.service.WalletCreditService;
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

class InternalWalletCreditControllerTest {

    private static final String INTERNAL_SECRET = "local-gateway-secret-change-me";
    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";
    private static final UUID PLAYER_ID =
            UUID.fromString("c43e2215-8c9f-4a68-a179-94fcf4274ae9");

    private final FakeWalletCreditService walletCreditService = new FakeWalletCreditService();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new InternalWalletCreditController(walletCreditService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new GatewayInternalAuthFilter(INTERNAL_SECRET))
            .build();

    @Test
    void creditWithoutInternalSecretReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/internal/wallets/credit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validRequestDelegatesOnceAndMapsFreshResult() throws Exception {
        UUID transactionId = UUID.randomUUID();
        walletCreditService.result = result(transactionId, false);

        mockMvc.perform(validCreditRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.playerId").value(PLAYER_ID.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.amount").value(50.00))
                .andExpect(jsonPath("$.balanceBefore").value(980.00))
                .andExpect(jsonPath("$.cash").value(1030.00))
                .andExpect(jsonPath("$.bonus").value(0))
                .andExpect(jsonPath("$.reference").value("manual-result-test-001"))
                .andExpect(jsonPath("$.duplicate").value(false));

        assertEquals(1, walletCreditService.callCount);
        assertEquals(PLAYER_ID, walletCreditService.command.playerId());
        assertEquals("EUR", walletCreditService.command.currency());
        assertEquals(new BigDecimal("50.00"), walletCreditService.command.amount());
        assertEquals("manual-result-test-001", walletCreditService.command.externalReference());
    }

    @Test
    void identicalDuplicateReturnsHttp200AndOriginalStoredResult() throws Exception {
        UUID transactionId = UUID.randomUUID();
        walletCreditService.result = result(transactionId, true);

        mockMvc.perform(validCreditRequest())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(transactionId.toString()))
                .andExpect(jsonPath("$.balanceBefore").value(980.00))
                .andExpect(jsonPath("$.cash").value(1030.00))
                .andExpect(jsonPath("$.duplicate").value(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"currency\":\"EUR\",\"amount\":50.00,\"reference\":\"win-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"\",\"amount\":50.00,\"reference\":\"win-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"reference\":\"win-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"amount\":0,\"reference\":\"win-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"amount\":-1,\"reference\":\"win-1\"}",
            "{\"playerId\":\"c43e2215-8c9f-4a68-a179-94fcf4274ae9\",\"currency\":\"EUR\",\"amount\":50.00,\"reference\":\"\"}"
    })
    void invalidRequestReturnsBadRequest(String requestBody) throws Exception {
        mockMvc.perform(post("/internal/wallets/credit")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void walletNotFoundReturnsNotFound() throws Exception {
        walletCreditService.exception = WalletNotFoundException.forPlayerAndCurrency(PLAYER_ID, "EUR");

        mockMvc.perform(validCreditRequest())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void idempotencyConflictIncludingDebitReferenceReturnsConflict() throws Exception {
        walletCreditService.exception =
                new WalletTransactionIdempotencyConflictException("manual-result-test-001");

        mockMvc.perform(validCreditRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void unexpectedFailureReturnsSafeInternalServerError() throws Exception {
        walletCreditService.exception = new RuntimeException("constraint uk_secret and SQL details");

        mockMvc.perform(validCreditRequest())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Unexpected wallet service error"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validCreditRequest() {
        return post("/internal/wallets/credit")
                .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequest());
    }

    private String validRequest() {
        return """
                {
                  "playerId": "c43e2215-8c9f-4a68-a179-94fcf4274ae9",
                  "currency": "EUR",
                  "amount": 50.00,
                  "reference": "manual-result-test-001"
                }
                """;
    }

    private WalletCreditResult result(UUID transactionId, boolean duplicate) {
        return new WalletCreditResult(
                transactionId,
                PLAYER_ID,
                "EUR",
                new BigDecimal("50.00"),
                new BigDecimal("980.00"),
                new BigDecimal("1030.00"),
                "manual-result-test-001",
                duplicate
        );
    }

    private static class FakeWalletCreditService extends WalletCreditService {
        private WalletCreditCommand command;
        private WalletCreditResult result;
        private RuntimeException exception;
        private int callCount;

        FakeWalletCreditService() {
            super(null, null, null);
        }

        @Override
        public WalletCreditResult credit(WalletCreditCommand command) {
            this.command = command;
            callCount++;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
