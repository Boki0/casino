package com.boki0.casino.game.api;

import com.boki0.casino.game.exception.GameExceptionHandler;
import com.boki0.casino.game.security.GatewayLaunchAuthenticationInterceptor;
import com.boki0.casino.game.service.GameCatalogService;
import com.boki0.casino.game.service.GameLaunchResult;
import com.boki0.casino.game.service.GameLaunchService;
import com.boki0.casino.game.service.GameSessionLifecycleService;
import com.boki0.casino.game.service.PrepareGameLaunchCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GameControllerTest {

    private static final String INTERNAL_SECRET = "test-gateway-secret";

    private GameCatalogService gameCatalogService;
    private GameLaunchService gameLaunchService;
    private GameSessionLifecycleService gameSessionLifecycleService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gameCatalogService = mock(GameCatalogService.class);
        gameLaunchService = mock(GameLaunchService.class);
        gameSessionLifecycleService = mock(GameSessionLifecycleService.class);
        GameController controller = new GameController(
                gameCatalogService,
                gameLaunchService,
                gameSessionLifecycleService
        );
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new GatewayLaunchAuthenticationInterceptor(INTERNAL_SECRET))
                .setControllerAdvice(new GameExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void authenticatedLaunchShouldReturnSafeResponseAndUseGatewayPlayerId() throws Exception {
        UUID gameId = UUID.randomUUID();
        UUID authenticatedPlayerId = UUID.randomUUID();
        UUID attackerPlayerId = UUID.randomUUID();
        UUID localSessionId = UUID.randomUUID();
        String launchUrl = "https://provider.example/games/lucky-seven/?sessionId=provider-session-id";
        when(gameLaunchService.launch(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new GameLaunchResult(localSessionId, launchUrl));

        mockMvc.perform(post("/games/{gameId}/launch", gameId)
                        .header("X-Internal-Gateway-Secret", INTERNAL_SECRET)
                        .header("X-Auth-User-Id", authenticatedPlayerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\",\"playerId\":\"" + attackerPlayerId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sessionId").value(localSessionId.toString()))
                .andExpect(jsonPath("$.launchUrl").value(launchUrl))
                .andExpect(jsonPath("$.rawToken").doesNotExist())
                .andExpect(jsonPath("$.tokenHash").doesNotExist())
                .andExpect(jsonPath("$.playerId").doesNotExist());

        ArgumentCaptor<PrepareGameLaunchCommand> commandCaptor =
                ArgumentCaptor.forClass(PrepareGameLaunchCommand.class);
        verify(gameLaunchService).launch(commandCaptor.capture());
        PrepareGameLaunchCommand command = commandCaptor.getValue();
        assertEquals(gameId, command.gameId());
        assertEquals(authenticatedPlayerId, command.playerId());
        assertEquals("EUR", command.currency());
    }

    @Test
    void launchShouldRejectMissingCurrency() throws Exception {
        mockMvc.perform(post("/games/{gameId}/launch", UUID.randomUUID())
                        .header("X-Internal-Gateway-Secret", INTERNAL_SECRET)
                        .header("X-Auth-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(gameLaunchService, never()).launch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void launchShouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(post("/games/{gameId}/launch", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"EUR\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized game request"));

        verify(gameLaunchService, never()).launch(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authenticatedOwnerShouldCloseSessionWithoutResponseBody() throws Exception {
        UUID sessionId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        mockMvc.perform(post("/games/sessions/{sessionId}/close", sessionId)
                        .header("X-Internal-Gateway-Secret", INTERNAL_SECRET)
                        .header("X-Auth-User-Id", playerId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(gameSessionLifecycleService).close(sessionId, playerId);
    }

    @Test
    void closeShouldRejectAnonymousRequest() throws Exception {
        mockMvc.perform(post("/games/sessions/{sessionId}/close", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized game request"));

        verify(gameSessionLifecycleService, never()).close(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void publicGameCatalogShouldRemainAccessible() throws Exception {
        when(gameCatalogService.getEnabledGames()).thenReturn(List.of());

        mockMvc.perform(get("/games"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(gameCatalogService).getEnabledGames();
    }
}
