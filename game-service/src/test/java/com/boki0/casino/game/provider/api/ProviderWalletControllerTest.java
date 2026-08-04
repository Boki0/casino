package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.auth.ProviderSessionAuthenticationService;
import com.boki0.casino.game.provider.bet.ProviderBetService;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProviderWalletControllerTest {

    private ProviderSessionAuthenticationService authenticationService;
    private ProviderBetService betService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authenticationService = mock(ProviderSessionAuthenticationService.class);
        betService = mock(ProviderBetService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new ProviderWalletController(authenticationService, betService)
                )
                .setControllerAdvice(new ProviderCallbackExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnProviderCompatibleSuccessWithoutTokenData() throws Exception {
        UUID userId = UUID.randomUUID();
        when(authenticationService.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(ProviderAuthenticateResponse.success(
                        userId,
                        "EUR",
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO
                ));

        mockMvc.perform(post("/api/provider-wallet/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "operator-generated-token",
                                  "providerCode": "NOVA_REELS",
                                  "gameCode": "NOVA_SEVEN",
                                  "sessionId": "provider-session-id"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.cash").value(1000.00))
                .andExpect(jsonPath("$.bonus").value(0))
                .andExpect(jsonPath("$.error").value(0))
                .andExpect(jsonPath("$.description").value("Success"))
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist());
    }

    @Test
    void shouldReturnStableErrorForInvalidRequest() throws Exception {
        mockMvc.perform(post("/api/provider-wallet/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(1000))
                .andExpect(jsonPath("$.description").value("Invalid authentication request"));
    }
}
