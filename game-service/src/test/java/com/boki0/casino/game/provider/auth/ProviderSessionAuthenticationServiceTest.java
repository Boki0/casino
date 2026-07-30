package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.provider.dto.ProviderAuthenticateRequest;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.service.GameSessionTokenService;
import com.boki0.casino.game.wallet.client.WalletBalanceClient;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProviderSessionAuthenticationServiceTest {

    private static final String RAW_TOKEN = "operator-generated-token";
    private static final String TOKEN_HASH = "a".repeat(64);

    private GameSessionTokenService tokenService;
    private ProviderSessionValidationService validationService;
    private WalletBalanceClient walletBalanceClient;
    private ProviderSessionAuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        tokenService = mock(GameSessionTokenService.class);
        validationService = mock(ProviderSessionValidationService.class);
        walletBalanceClient = mock(WalletBalanceClient.class);
        authenticationService = new ProviderSessionAuthenticationService(
                tokenService,
                validationService,
                walletBalanceClient
        );
    }

    @Test
    void shouldHashTokenValidateSessionAndReturnWalletBalance() {
        ProviderAuthenticateRequest request = request();
        UUID playerId = UUID.randomUUID();
        ValidatedProviderSession session = new ValidatedProviderSession(
                UUID.randomUUID(),
                playerId,
                "EUR"
        );
        when(tokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(validationService.validate(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id"))
                .thenReturn(session);
        when(walletBalanceClient.getBalance(playerId)).thenReturn(
                new WalletBalanceResponse(playerId, new BigDecimal("1000.00"), "EUR")
        );

        ProviderAuthenticateResponse response = authenticationService.authenticate(request);

        assertEquals(playerId, response.userId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("1000.00"), response.cash());
        assertEquals(BigDecimal.ZERO, response.bonus());
        assertEquals(0, response.error());
        assertEquals("Success", response.description());
        assertFalse(response.toString().contains(RAW_TOKEN));

        InOrder flow = inOrder(tokenService, validationService, walletBalanceClient);
        flow.verify(tokenService).hashToken(RAW_TOKEN);
        flow.verify(validationService).validate(
                TOKEN_HASH,
                "NOVA_REELS",
                "NOVA_SEVEN",
                "provider-session-id"
        );
        flow.verify(walletBalanceClient).getBalance(playerId);
    }

    @Test
    void shouldRejectWalletCurrencyMismatch() {
        ProviderAuthenticateRequest request = request();
        UUID playerId = UUID.randomUUID();
        when(tokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(validationService.validate(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id"))
                .thenReturn(new ValidatedProviderSession(UUID.randomUUID(), playerId, "EUR"));
        when(walletBalanceClient.getBalance(playerId)).thenReturn(
                new WalletBalanceResponse(playerId, BigDecimal.TEN, "CREDITS")
        );

        assertThrows(
                ProviderAuthenticationException.class,
                () -> authenticationService.authenticate(request)
        );
    }

    @Test
    void shouldNotReturnSuccessWhenWalletServiceFails() {
        ProviderAuthenticateRequest request = request();
        UUID playerId = UUID.randomUUID();
        when(tokenService.hashToken(RAW_TOKEN)).thenReturn(TOKEN_HASH);
        when(validationService.validate(TOKEN_HASH, "NOVA_REELS", "NOVA_SEVEN", "provider-session-id"))
                .thenReturn(new ValidatedProviderSession(UUID.randomUUID(), playerId, "EUR"));
        when(walletBalanceClient.getBalance(playerId))
                .thenThrow(new WalletClientException("Wallet service is unavailable"));

        ProviderAuthenticationException exception = assertThrows(
                ProviderAuthenticationException.class,
                () -> authenticationService.authenticate(request)
        );

        assertEquals(2001, exception.getErrorCode());
    }

    private ProviderAuthenticateRequest request() {
        return new ProviderAuthenticateRequest(
                RAW_TOKEN,
                "NOVA_REELS",
                "NOVA_SEVEN",
                "provider-session-id"
        );
    }
}
