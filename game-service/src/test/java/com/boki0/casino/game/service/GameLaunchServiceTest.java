package com.boki0.casino.game.service;

import com.boki0.casino.game.provider.client.ProviderCatalogClient;
import com.boki0.casino.game.provider.dto.ProviderLaunchRequest;
import com.boki0.casino.game.provider.dto.ProviderLaunchResponse;
import com.boki0.casino.game.provider.exception.ProviderCatalogException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameLaunchServiceTest {

    private static final String RAW_TOKEN = "operator-generated-token";

    private GameLaunchPreparationService preparationService;
    private ProviderCatalogClient providerClient;
    private GameSessionLifecycleService lifecycleService;
    private GameLaunchService gameLaunchService;

    @BeforeEach
    void setUp() {
        preparationService = mock(GameLaunchPreparationService.class);
        providerClient = mock(ProviderCatalogClient.class);
        lifecycleService = mock(GameSessionLifecycleService.class);
        gameLaunchService = new GameLaunchService(
                preparationService,
                providerClient,
                lifecycleService
        );
    }

    @Test
    void shouldPrepareLaunchCallProviderAndActivateSession() {
        PrepareGameLaunchCommand command = command();
        PreparedGameLaunch prepared = prepared(command);
        ProviderLaunchResponse providerResponse = new ProviderLaunchResponse(
                "provider-session-id",
                "https://provider.example/games/lucky-seven/?sessionId=provider-session-id"
        );
        when(preparationService.prepare(command)).thenReturn(prepared);
        when(providerClient.launchGame(org.mockito.ArgumentMatchers.any())).thenReturn(providerResponse);

        GameLaunchResult result = gameLaunchService.launch(command);

        ArgumentCaptor<ProviderLaunchRequest> requestCaptor =
                ArgumentCaptor.forClass(ProviderLaunchRequest.class);
        verify(providerClient, times(1)).launchGame(requestCaptor.capture());
        ProviderLaunchRequest request = requestCaptor.getValue();
        assertEquals("NOVA_REELS", request.providerCode());
        assertEquals("NOVA_SEVEN", request.gameCode());
        assertEquals(command.playerId().toString(), request.playerId());
        assertEquals("EUR", request.currency());
        assertEquals(RAW_TOKEN, request.token());
        assertEquals("REAL", request.mode());

        assertEquals(prepared.localSessionId(), result.localSessionId());
        assertEquals(providerResponse.launchUrl(), result.launchUrl());
        assertFalse(result.toString().contains(RAW_TOKEN));

        InOrder flow = inOrder(preparationService, providerClient, lifecycleService);
        flow.verify(preparationService).prepare(command);
        flow.verify(providerClient).launchGame(request);
        flow.verify(lifecycleService).activate(
                prepared.localSessionId(),
                providerResponse.sessionId()
        );
    }

    @Test
    void shouldMarkSessionFailedAndRethrowProviderFailure() {
        PrepareGameLaunchCommand command = command();
        PreparedGameLaunch prepared = prepared(command);
        ProviderCatalogException providerFailure =
                new ProviderCatalogException("Provider launch failed with HTTP status 500");
        when(preparationService.prepare(command)).thenReturn(prepared);
        when(providerClient.launchGame(org.mockito.ArgumentMatchers.any())).thenThrow(providerFailure);

        ProviderCatalogException thrown = assertThrows(
                ProviderCatalogException.class,
                () -> gameLaunchService.launch(command)
        );

        assertSame(providerFailure, thrown);
        verify(lifecycleService).markFailed(prepared.localSessionId());
    }

    @Test
    void shouldPreserveProviderFailureWhenMarkFailedAlsoFails() {
        PrepareGameLaunchCommand command = command();
        PreparedGameLaunch prepared = prepared(command);
        ProviderCatalogException providerFailure =
                new ProviderCatalogException("Provider launch communication failed");
        IllegalStateException lifecycleFailure =
                new IllegalStateException("Local session update failed");
        when(preparationService.prepare(command)).thenReturn(prepared);
        when(providerClient.launchGame(org.mockito.ArgumentMatchers.any())).thenThrow(providerFailure);
        org.mockito.Mockito.doThrow(lifecycleFailure)
                .when(lifecycleService)
                .markFailed(prepared.localSessionId());

        ProviderCatalogException thrown = assertThrows(
                ProviderCatalogException.class,
                () -> gameLaunchService.launch(command)
        );

        assertSame(providerFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(lifecycleFailure, thrown.getSuppressed()[0]);
    }

    private PrepareGameLaunchCommand command() {
        return new PrepareGameLaunchCommand(UUID.randomUUID(), UUID.randomUUID(), "EUR");
    }

    private PreparedGameLaunch prepared(PrepareGameLaunchCommand command) {
        return new PreparedGameLaunch(
                UUID.randomUUID(),
                RAW_TOKEN,
                "NOVA_REELS",
                "NOVA_SEVEN",
                command.playerId(),
                command.currency(),
                Instant.now().plusSeconds(900)
        );
    }
}
