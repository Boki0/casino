package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.dto.InternalWalletBalanceResponse;
import com.boki0.casino.wallet.exception.GlobalExceptionHandler;
import com.boki0.casino.wallet.exception.WalletNotFoundException;
import com.boki0.casino.wallet.repository.WalletRepository;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import com.boki0.casino.wallet.security.GatewayInternalAuthFilter;
import com.boki0.casino.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalWalletControllerTest {

    private static final String INTERNAL_SECRET = "local-gateway-secret-change-me";
    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";

    private final FakeWalletService walletService = new FakeWalletService();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new InternalWalletController(walletService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new GatewayInternalAuthFilter(INTERNAL_SECRET))
            .build();

    @Test
    void getBalance_withoutInternalSecretReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/internal/wallets/balance")
                        .param("playerId", UUID.randomUUID().toString())
                        .param("currency", "EUR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getBalance_returnsFocusedInternalResponse() throws Exception {
        UUID playerId = UUID.randomUUID();
        walletService.response = new InternalWalletBalanceResponse(
                playerId,
                "EUR",
                new BigDecimal("1000.00"),
                BigDecimal.ZERO
        );

        mockMvc.perform(get("/internal/wallets/balance")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                        .param("playerId", playerId.toString())
                        .param("currency", "eur"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value(playerId.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.cash").value(1000.00))
                .andExpect(jsonPath("$.bonus").value(0));
    }

    @Test
    void getBalance_missingWalletReturnsNotFound() throws Exception {
        UUID playerId = UUID.randomUUID();
        walletService.exception = WalletNotFoundException.forPlayerAndCurrency(playerId, "EUR");

        mockMvc.perform(get("/internal/wallets/balance")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                        .param("playerId", playerId.toString())
                        .param("currency", "EUR"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    private static class FakeWalletService extends WalletService {
        private InternalWalletBalanceResponse response;
        private RuntimeException exception;

        FakeWalletService() {
            super(nullRepository(), nullTransactionRepository());
        }

        @Override
        public InternalWalletBalanceResponse getInternalBalance(UUID playerId, String currency) {
            if (exception != null) {
                throw exception;
            }
            return response;
        }

        private static WalletRepository nullRepository() {
            return null;
        }

        private static WalletTransactionRepository nullTransactionRepository() {
            return null;
        }
    }
}
