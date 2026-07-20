package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.dto.WalletResponse;
import com.boki0.casino.wallet.entity.WalletStatus;
import com.boki0.casino.wallet.exception.GlobalExceptionHandler;
import com.boki0.casino.wallet.repository.WalletRepository;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import com.boki0.casino.wallet.security.GatewayInternalAuthFilter;
import com.boki0.casino.wallet.service.WalletService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WalletControllerTest {

    private static final String INTERNAL_SECRET = "local-gateway-secret-change-me";
    private static final String HEADER_INTERNAL_GATEWAY_SECRET = "X-Internal-Gateway-Secret";
    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";

    private final FakeWalletService walletService = new FakeWalletService();
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new WalletController(walletService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .addFilters(new GatewayInternalAuthFilter(INTERNAL_SECRET))
            .build();

    @Test
    void getCurrentUserWallet_withoutInternalGatewaySecret_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/wallet/me")
                        .header(HEADER_AUTH_USER_ID, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserWallet_withInvalidInternalGatewaySecret_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/wallet/me")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, "wrong-secret")
                        .header(HEADER_AUTH_USER_ID, UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getCurrentUserWallet_withValidInternalGatewaySecretButMissingAuthUserId_shouldReturnBadRequest()
            throws Exception {
        mockMvc.perform(get("/wallet/me")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing X-Auth-User-Id header"));
    }

    @Test
    void getCurrentUserWallet_withValidHeaders_shouldReturnWalletResponse() throws Exception {
        UUID authUserId = UUID.randomUUID();
        walletService.response = new WalletResponse(
                UUID.randomUUID(),
                authUserId,
                new BigDecimal("125.00"),
                "CREDITS",
                WalletStatus.ACTIVE,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now()
        );

        mockMvc.perform(get("/wallet/me")
                        .header(HEADER_INTERNAL_GATEWAY_SECRET, INTERNAL_SECRET)
                        .header(HEADER_AUTH_USER_ID, authUserId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authUserId").value(authUserId.toString()))
                .andExpect(jsonPath("$.balance").value(125.00))
                .andExpect(jsonPath("$.currency").value("CREDITS"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    private static class FakeWalletService extends WalletService {
        private WalletResponse response;

        FakeWalletService() {
            super(nullRepository(), nullTransactionRepository());
        }

        @Override
        public WalletResponse getWalletByAuthUserId(UUID authUserId) {
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
