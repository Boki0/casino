package com.boki0.casino.game.provider.api;

import com.boki0.casino.game.provider.auth.ProviderSessionAuthenticationService;
import com.boki0.casino.game.provider.bet.ProviderBetService;
import com.boki0.casino.game.provider.dto.ProviderResultRequest;
import com.boki0.casino.game.provider.dto.ProviderResultResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.provider.result.ProviderResultService;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProviderResultControllerTest {

    private static final UUID PLAYER_ID =
            UUID.fromString("6a71d2e2-a81a-490a-adf9-d5896a20c483");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("7035a82f-146a-42a0-ab95-07d867854ada");

    private ProviderResultService resultService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        resultService = mock(ProviderResultService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProviderWalletController(
                        mock(ProviderSessionAuthenticationService.class),
                        mock(ProviderBetService.class),
                        resultService
                ))
                .setControllerAdvice(new ProviderCallbackExceptionHandler())
                .build();
    }

    @Test
    void validAnonymousCallbackDelegatesAndReturnsExactProviderResponse() throws Exception {
        when(resultService.process(any())).thenReturn(successResponse());

        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("50.00")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.cash").value(1030.00))
                .andExpect(jsonPath("$.bonus").value(0.00))
                .andExpect(jsonPath("$.error").isNumber())
                .andExpect(jsonPath("$.error").value(0))
                .andExpect(jsonPath("$.description").value("Success"));

        ArgumentCaptor<ProviderResultRequest> requestCaptor =
                ArgumentCaptor.forClass(ProviderResultRequest.class);
        verify(resultService).process(requestCaptor.capture());
        assertEquals(PLAYER_ID, requestCaptor.getValue().userId());
        assertEquals(new BigDecimal("50.00"), requestCaptor.getValue().amount());
        assertEquals("provider-result-reference", requestCaptor.getValue().reference());
        assertEquals("operator-generated-token", requestCaptor.getValue().token());
    }

    @Test
    void zeroResultIsAcceptedAndReturnedUnchanged() throws Exception {
        when(resultService.process(any())).thenReturn(new ProviderResultResponse(
                TRANSACTION_ID, "EUR", new BigDecimal("980.00"), BigDecimal.ZERO, 0, "Success"
        ));

        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("0")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cash").value(980.00))
                .andExpect(jsonPath("$.error").value(0));

        verify(resultService).process(any(ProviderResultRequest.class));
    }

    @Test
    void identicalDuplicateResponseRemainsHttp200Success() throws Exception {
        when(resultService.process(any())).thenReturn(successResponse());

        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(TRANSACTION_ID.toString()))
                .andExpect(jsonPath("$.cash").value(1030.00))
                .andExpect(jsonPath("$.error").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"userId\":\"6a71d2e2-a81a-490a-adf9-d5896a20c483\",\"gameId\":\"NOVA_SEVEN\",\"roundId\":\"round\",\"amount\":0,\"reference\":\"ref\",\"providerId\":\"NOVA_REELS\",\"timestamp\":1780000000000,\"token\":\"\"}",
            "{\"userId\":\"6a71d2e2-a81a-490a-adf9-d5896a20c483\",\"gameId\":\"NOVA_SEVEN\",\"roundId\":\"round\",\"amount\":0,\"reference\":\"ref\",\"providerId\":\"\",\"timestamp\":1780000000000,\"token\":\"token\"}",
            "{\"userId\":\"6a71d2e2-a81a-490a-adf9-d5896a20c483\",\"gameId\":\"\",\"roundId\":\"round\",\"amount\":0,\"reference\":\"ref\",\"providerId\":\"NOVA_REELS\",\"timestamp\":1780000000000,\"token\":\"token\"}",
            "{\"userId\":\"6a71d2e2-a81a-490a-adf9-d5896a20c483\",\"gameId\":\"NOVA_SEVEN\",\"roundId\":\"\",\"amount\":0,\"reference\":\"ref\",\"providerId\":\"NOVA_REELS\",\"timestamp\":1780000000000,\"token\":\"token\"}",
            "{\"userId\":\"6a71d2e2-a81a-490a-adf9-d5896a20c483\",\"gameId\":\"NOVA_SEVEN\",\"roundId\":\"round\",\"amount\":0,\"reference\":\"\",\"providerId\":\"NOVA_REELS\",\"timestamp\":1780000000000,\"token\":\"token\"}",
            "{\"userId\":\"6a71d2e2-a81a-490a-adf9-d5896a20c483\",\"gameId\":\"NOVA_SEVEN\",\"roundId\":\"round\",\"amount\":-1,\"reference\":\"ref\",\"providerId\":\"NOVA_REELS\",\"timestamp\":1780000000000,\"token\":\"token\"}"
    })
    void invalidRequestReturnsProviderErrorWithoutCallingService(String requestBody) throws Exception {
        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").doesNotExist())
                .andExpect(jsonPath("$.cash").value(0))
                .andExpect(jsonPath("$.bonus").value(0))
                .andExpect(jsonPath("$.error").value(1000))
                .andExpect(jsonPath("$.description").value("Invalid Result request"));

        verify(resultService, never()).process(any());
    }

    @Test
    void invalidSessionReturnsProviderCompatibleError() throws Exception {
        when(resultService.process(any())).thenThrow(ProviderAuthenticationException.rejected());

        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(1001))
                .andExpect(jsonPath("$.description").value("Session authentication failed"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "WALLET_NOT_FOUND:2003:Wallet not found",
            "IDEMPOTENCY_CONFLICT:2004:Result reference conflict",
            "UNAVAILABLE:2001:Result could not be processed",
            "SERVICE_FAILURE:2001:Result could not be processed",
            "INVALID_RESPONSE:2001:Result could not be processed"
    })
    void walletFailuresReturnStableProviderErrors(String scenario) throws Exception {
        String[] values = scenario.split(":", 3);
        WalletClientException.Category category = WalletClientException.Category.valueOf(values[0]);
        when(resultService.process(any()))
                .thenThrow(new WalletClientException("internal wallet detail", category));

        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("50.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(Integer.parseInt(values[1])))
                .andExpect(jsonPath("$.description").value(values[2]))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("internal wallet detail")
                )));
    }

    @Test
    void unexpectedFailureReturnsSafeResponseWithoutStackTrace() throws Exception {
        when(resultService.process(any()))
                .thenThrow(new IllegalStateException("SQL secret stack detail"));

        mockMvc.perform(post("/api/provider-wallet/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest("50.00")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(2001))
                .andExpect(jsonPath("$.description").value("Result could not be processed"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("SQL secret stack detail")
                )));
    }

    private ProviderResultResponse successResponse() {
        return new ProviderResultResponse(
                TRANSACTION_ID,
                "EUR",
                new BigDecimal("1030.00"),
                new BigDecimal("0.00"),
                0,
                "Success"
        );
    }

    private String validRequest(String amount) {
        return """
                {
                  "userId": "6a71d2e2-a81a-490a-adf9-d5896a20c483",
                  "gameId": "NOVA_SEVEN",
                  "roundId": "provider-round-id",
                  "amount": %s,
                  "reference": "provider-result-reference",
                  "providerId": "NOVA_REELS",
                  "timestamp": 1780000001000,
                  "roundDetails": "spin",
                  "platform": "WEB",
                  "token": "operator-generated-token"
                }
                """.formatted(amount);
    }
}
