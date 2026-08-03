package com.boki0.casino.game.provider.bet;

import com.boki0.casino.game.provider.auth.ProviderBetSessionValidator;
import com.boki0.casino.game.provider.auth.ValidatedProviderBet;
import com.boki0.casino.game.provider.dto.ProviderBetRequest;
import com.boki0.casino.game.provider.dto.ProviderBetResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.repository.GameSessionRepository;
import com.boki0.casino.game.wallet.client.WalletBalanceClient;
import com.boki0.casino.game.wallet.dto.WalletDebitRequest;
import com.boki0.casino.game.wallet.dto.WalletDebitResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProviderBetServiceTest {

    private static final String RAW_TOKEN = "operator-generated-token";
    private static final UUID PLAYER_ID =
            UUID.fromString("6a71d2e2-a81a-490a-adf9-d5896a20c483");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("7035a82f-146a-42a0-ab95-07d867854ada");

    private ProviderBetSessionValidator sessionValidator;
    private WalletBalanceClient walletClient;
    private ProviderBetService betService;

    @BeforeEach
    void setUp() {
        sessionValidator = mock(ProviderBetSessionValidator.class);
        walletClient = mock(WalletBalanceClient.class);
        betService = new ProviderBetService(sessionValidator, walletClient);
    }

    @Test
    void validBetUsesTrustedValuesAndReturnsSuccessfulProviderResponse() {
        ProviderBetRequest request = request();
        ValidatedProviderBet validatedBet = validatedBet();
        when(sessionValidator.validate(request)).thenReturn(validatedBet);
        when(walletClient.debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class)))
                .thenReturn(debitResponse(false));

        ProviderBetResponse response = betService.process(request);

        ArgumentCaptor<WalletDebitRequest> debitCaptor =
                ArgumentCaptor.forClass(WalletDebitRequest.class);
        InOrder flow = inOrder(sessionValidator, walletClient);
        flow.verify(sessionValidator).validate(request);
        flow.verify(walletClient).debit(debitCaptor.capture());
        verifyNoMoreInteractions(sessionValidator, walletClient);

        WalletDebitRequest debitRequest = debitCaptor.getValue();
        assertEquals(validatedBet.playerId(), debitRequest.playerId());
        assertEquals(validatedBet.currency(), debitRequest.currency());
        assertEquals(validatedBet.amount(), debitRequest.amount());
        assertEquals(validatedBet.reference(), debitRequest.reference());
        assertFalse(debitRequest.toString().contains(RAW_TOKEN));

        assertEquals(TRANSACTION_ID, response.transactionId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("99.00"), response.cash());
        assertEquals(new BigDecimal("0.00"), response.bonus());
        assertEquals(BigDecimal.ZERO, response.usedPromo());
        assertEquals(0, response.error());
        assertEquals("Success", response.description());
    }

    @Test
    void identicalDuplicateIsSuccessfulAndReturnsOriginalTransactionAndBalance() {
        ProviderBetRequest request = request();
        when(sessionValidator.validate(request)).thenReturn(validatedBet());
        when(walletClient.debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class)))
                .thenReturn(debitResponse(true));

        ProviderBetResponse response = betService.process(request);

        assertEquals(TRANSACTION_ID, response.transactionId());
        assertEquals(new BigDecimal("99.00"), response.cash());
        assertEquals(0, response.error());
        assertEquals("Success", response.description());
        verify(walletClient).debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class));
    }

    @ParameterizedTest
    @EnumSource(
            value = WalletClientException.Category.class,
            names = {
                    "INSUFFICIENT_BALANCE",
                    "WALLET_NOT_FOUND",
                    "IDEMPOTENCY_CONFLICT",
                    "SERVICE_FAILURE",
                    "UNAVAILABLE"
            }
    )
    void walletFailuresArePropagatedWithoutReturningSuccess(
            WalletClientException.Category category
    ) {
        ProviderBetRequest request = request();
        WalletClientException walletFailure = new WalletClientException("Wallet debit failed", category);
        when(sessionValidator.validate(request)).thenReturn(validatedBet());
        when(walletClient.debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class)))
                .thenThrow(walletFailure);

        WalletClientException thrown = assertThrows(
                WalletClientException.class,
                () -> betService.process(request)
        );

        assertSame(walletFailure, thrown);
        assertEquals(category, thrown.getCategory());
        verify(walletClient).debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class));
    }

    @Test
    void invalidWalletResponseDoesNotReturnSuccess() {
        ProviderBetRequest request = request();
        when(sessionValidator.validate(request)).thenReturn(validatedBet());
        when(walletClient.debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class)))
                .thenReturn(new WalletDebitResponse(
                        null,
                        PLAYER_ID,
                        "EUR",
                        new BigDecimal("1.00"),
                        new BigDecimal("99.00"),
                        BigDecimal.ZERO,
                        "provider-bet-reference",
                        false
                ));

        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> betService.process(request)
        );

        assertEquals(WalletClientException.Category.INVALID_RESPONSE, exception.getCategory());
    }

    @Test
    void walletResponseMustMatchTrustedPlayerCurrencyAmountAndReference() {
        assertInvalidResponse(new WalletDebitResponse(
                TRANSACTION_ID,
                UUID.randomUUID(),
                "EUR",
                amount("1.00"),
                amount("99.00"),
                BigDecimal.ZERO,
                "provider-bet-reference",
                false
        ));
        assertInvalidResponse(new WalletDebitResponse(
                TRANSACTION_ID,
                PLAYER_ID,
                "USD",
                amount("1.00"),
                amount("99.00"),
                BigDecimal.ZERO,
                "provider-bet-reference",
                false
        ));
        assertInvalidResponse(new WalletDebitResponse(
                TRANSACTION_ID,
                PLAYER_ID,
                "EUR",
                amount("2.00"),
                amount("99.00"),
                BigDecimal.ZERO,
                "provider-bet-reference",
                false
        ));
        assertInvalidResponse(new WalletDebitResponse(
                TRANSACTION_ID,
                PLAYER_ID,
                "EUR",
                amount("1.00"),
                amount("99.00"),
                BigDecimal.ZERO,
                "other-reference",
                false
        ));
    }

    @Test
    void numericallyEqualAmountWithDifferentScaleIsAccepted() {
        ProviderBetRequest request = request();
        when(sessionValidator.validate(request)).thenReturn(validatedBet());
        WalletDebitResponse response = debitResponse(false);
        when(walletClient.debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class)))
                .thenReturn(new WalletDebitResponse(
                        response.transactionId(),
                        response.playerId(),
                        response.currency(),
                        new BigDecimal("1.0"),
                        response.cash(),
                        response.bonus(),
                        response.reference(),
                        response.duplicate()
                ));

        ProviderBetResponse providerResponse = betService.process(request);

        assertEquals(0, providerResponse.error());
    }

    @Test
    void validatorFailurePreventsWalletInvocation() {
        ProviderBetRequest request = request();
        ProviderAuthenticationException validationFailure = ProviderAuthenticationException.rejected();
        when(sessionValidator.validate(request)).thenThrow(validationFailure);

        ProviderAuthenticationException thrown = assertThrows(
                ProviderAuthenticationException.class,
                () -> betService.process(request)
        );

        assertSame(validationFailure, thrown);
        verify(walletClient, never()).debit(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void serviceHasNoDirectGameSessionRepositoryDependency() {
        Set<Class<?>> fieldTypes = Arrays.stream(ProviderBetService.class.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(fieldTypes.contains(GameSessionRepository.class));
    }

    private void assertInvalidResponse(WalletDebitResponse response) {
        ProviderBetRequest request = request();
        when(sessionValidator.validate(request)).thenReturn(validatedBet());
        when(walletClient.debit(org.mockito.ArgumentMatchers.any(WalletDebitRequest.class)))
                .thenReturn(response);

        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> betService.process(request)
        );

        assertEquals(WalletClientException.Category.INVALID_RESPONSE, exception.getCategory());
    }

    private ProviderBetRequest request() {
        return new ProviderBetRequest(
                PLAYER_ID,
                "NOVA_SEVEN",
                "provider-round-id",
                amount("1.00"),
                "provider-bet-reference",
                "NOVA_REELS",
                1780000000000L,
                "spin",
                "WEB",
                "en",
                RAW_TOKEN,
                "127.0.0.1"
        );
    }

    private ValidatedProviderBet validatedBet() {
        return new ValidatedProviderBet(
                UUID.randomUUID(),
                PLAYER_ID,
                "EUR",
                "NOVA_REELS",
                "NOVA_SEVEN",
                "provider-session-id",
                "provider-round-id",
                "provider-bet-reference",
                amount("1.00")
        );
    }

    private WalletDebitResponse debitResponse(boolean duplicate) {
        return new WalletDebitResponse(
                TRANSACTION_ID,
                PLAYER_ID,
                "EUR",
                amount("1.00"),
                amount("99.00"),
                BigDecimal.ZERO.setScale(2),
                "provider-bet-reference",
                duplicate
        );
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
