package com.boki0.casino.game.provider.result;

import com.boki0.casino.game.provider.auth.ProviderResultSessionValidator;
import com.boki0.casino.game.provider.auth.ValidatedProviderResult;
import com.boki0.casino.game.provider.dto.ProviderResultRequest;
import com.boki0.casino.game.provider.dto.ProviderResultResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.repository.GameSessionRepository;
import com.boki0.casino.game.wallet.client.WalletBalanceClient;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.dto.WalletCreditRequest;
import com.boki0.casino.game.wallet.dto.WalletCreditResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProviderResultServiceTest {

    private static final String RAW_TOKEN = "operator-generated-token";
    private static final UUID LOCAL_SESSION_ID =
            UUID.fromString("5298e967-1188-4d4d-91ae-d82fd5126092");
    private static final UUID PLAYER_ID =
            UUID.fromString("6a71d2e2-a81a-490a-adf9-d5896a20c483");
    private static final UUID TRANSACTION_ID =
            UUID.fromString("7035a82f-146a-42a0-ab95-07d867854ada");

    private ProviderResultSessionValidator sessionValidator;
    private WalletBalanceClient walletClient;
    private ProviderResultService resultService;

    @BeforeEach
    void setUp() {
        sessionValidator = mock(ProviderResultSessionValidator.class);
        walletClient = mock(WalletBalanceClient.class);
        resultService = new ProviderResultService(sessionValidator, walletClient);
    }

    @Test
    void positiveResultUsesTrustedValuesAndReturnsSuccessfulResponse() {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        ValidatedProviderResult validated = validated(new BigDecimal("50.00"));
        when(sessionValidator.validate(request)).thenReturn(validated);
        when(walletClient.credit(any(WalletCreditRequest.class))).thenReturn(creditResponse(false));

        ProviderResultResponse response = resultService.process(request);

        ArgumentCaptor<WalletCreditRequest> creditCaptor = ArgumentCaptor.forClass(WalletCreditRequest.class);
        InOrder flow = inOrder(sessionValidator, walletClient);
        flow.verify(sessionValidator).validate(request);
        flow.verify(walletClient).credit(creditCaptor.capture());
        verifyNoMoreInteractions(sessionValidator, walletClient);

        WalletCreditRequest creditRequest = creditCaptor.getValue();
        assertEquals(validated.playerId(), creditRequest.playerId());
        assertEquals(validated.currency(), creditRequest.currency());
        assertEquals(validated.amount(), creditRequest.amount());
        assertEquals(validated.reference(), creditRequest.reference());
        assertFalse(creditRequest.toString().contains(RAW_TOKEN));
        assertFalse(creditRequest.toString().contains(validated.roundId()));

        assertEquals(TRANSACTION_ID, response.transactionId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("150.00"), response.cash());
        assertEquals(new BigDecimal("0.00"), response.bonus());
        assertEquals(0, response.error());
        assertEquals("Success", response.description());
    }

    @Test
    void identicalDuplicateIsSuccessfulWithOriginalTransactionAndCash() {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        when(sessionValidator.validate(request)).thenReturn(validated(new BigDecimal("50.00")));
        when(walletClient.credit(any(WalletCreditRequest.class))).thenReturn(creditResponse(true));

        ProviderResultResponse response = resultService.process(request);

        assertEquals(TRANSACTION_ID, response.transactionId());
        assertEquals(new BigDecimal("150.00"), response.cash());
        assertEquals(0, response.error());
        assertEquals("Success", response.description());
        verify(walletClient).credit(any(WalletCreditRequest.class));
    }

    @Test
    void zeroResultReadsBalanceAndNeverCallsCredit() {
        ProviderResultRequest request = request(BigDecimal.ZERO);
        ValidatedProviderResult validated = validated(BigDecimal.ZERO);
        when(sessionValidator.validate(request)).thenReturn(validated);
        when(walletClient.getBalance(PLAYER_ID, "EUR")).thenReturn(balanceResponse());

        ProviderResultResponse response = resultService.process(request);

        assertEquals(LOCAL_SESSION_ID, response.transactionId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("100.00"), response.cash());
        assertEquals(new BigDecimal("0.00"), response.bonus());
        assertEquals(0, response.error());
        assertEquals("Success", response.description());
        verify(walletClient).getBalance(PLAYER_ID, "EUR");
        verify(walletClient, never()).credit(any());
    }

    @ParameterizedTest
    @EnumSource(
            value = WalletClientException.Category.class,
            names = {
                    "WALLET_NOT_FOUND",
                    "IDEMPOTENCY_CONFLICT",
                    "SERVICE_FAILURE",
                    "UNAVAILABLE",
                    "INVALID_RESPONSE"
            }
    )
    void positiveResultWalletFailuresArePropagated(WalletClientException.Category category) {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        WalletClientException failure = new WalletClientException("Wallet credit failed", category);
        when(sessionValidator.validate(request)).thenReturn(validated(new BigDecimal("50.00")));
        when(walletClient.credit(any(WalletCreditRequest.class))).thenThrow(failure);

        WalletClientException thrown = assertThrows(
                WalletClientException.class,
                () -> resultService.process(request)
        );

        assertSame(failure, thrown);
        assertEquals(category, thrown.getCategory());
    }

    @Test
    void validatorFailurePreventsEveryWalletInvocation() {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        ProviderAuthenticationException failure = ProviderAuthenticationException.rejected();
        when(sessionValidator.validate(request)).thenThrow(failure);

        assertSame(failure, assertThrows(
                ProviderAuthenticationException.class,
                () -> resultService.process(request)
        ));
        verifyNoMoreInteractions(walletClient);
    }

    @Test
    void incompleteCreditResponseDoesNotReturnSuccess() {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        when(sessionValidator.validate(request)).thenReturn(validated(new BigDecimal("50.00")));
        when(walletClient.credit(any(WalletCreditRequest.class))).thenReturn(new WalletCreditResponse(
                null, PLAYER_ID, "EUR", new BigDecimal("50.00"), new BigDecimal("100.00"),
                new BigDecimal("150.00"), BigDecimal.ZERO, "provider-result-reference", false
        ));

        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> resultService.process(request)
        );

        assertEquals(WalletClientException.Category.INVALID_RESPONSE, exception.getCategory());
    }

    @Test
    void creditResponseMustMatchTrustedPlayerCurrencyAmountAndReference() {
        assertInvalidCredit(new WalletCreditResponse(
                TRANSACTION_ID, UUID.randomUUID(), "EUR", amount("50"), amount("100"),
                amount("150"), BigDecimal.ZERO, "provider-result-reference", false
        ));
        assertInvalidCredit(new WalletCreditResponse(
                TRANSACTION_ID, PLAYER_ID, "USD", amount("50"), amount("100"),
                amount("150"), BigDecimal.ZERO, "provider-result-reference", false
        ));
        assertInvalidCredit(new WalletCreditResponse(
                TRANSACTION_ID, PLAYER_ID, "EUR", amount("51"), amount("100"),
                amount("150"), BigDecimal.ZERO, "provider-result-reference", false
        ));
        assertInvalidCredit(new WalletCreditResponse(
                TRANSACTION_ID, PLAYER_ID, "EUR", amount("50"), amount("100"),
                amount("150"), BigDecimal.ZERO, "other-reference", false
        ));
    }

    @Test
    void numericallyEqualCreditAmountWithDifferentScaleIsAccepted() {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        when(sessionValidator.validate(request)).thenReturn(validated(new BigDecimal("50.00")));
        WalletCreditResponse response = creditResponse(false);
        when(walletClient.credit(any(WalletCreditRequest.class))).thenReturn(new WalletCreditResponse(
                response.transactionId(), response.playerId(), response.currency(), new BigDecimal("50.0"),
                response.balanceBefore(), response.cash(), response.bonus(), response.reference(), false
        ));

        assertEquals(0, resultService.process(request).error());
    }

    @Test
    void invalidZeroBalanceResponseDoesNotReturnSuccess() {
        ProviderResultRequest request = request(BigDecimal.ZERO);
        when(sessionValidator.validate(request)).thenReturn(validated(BigDecimal.ZERO));
        when(walletClient.getBalance(PLAYER_ID, "EUR")).thenReturn(new WalletBalanceResponse(
                PLAYER_ID, "EUR", null, BigDecimal.ZERO
        ));

        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> resultService.process(request)
        );

        assertEquals(WalletClientException.Category.INVALID_RESPONSE, exception.getCategory());
    }

    @Test
    void serviceHasNoRepositoryOrTransactionBoundary() throws Exception {
        Set<Class<?>> fieldTypes = Arrays.stream(ProviderResultService.class.getDeclaredFields())
                .map(field -> field.getType())
                .collect(java.util.stream.Collectors.toSet());

        assertFalse(fieldTypes.contains(GameSessionRepository.class));
        assertNull(ProviderResultService.class
                .getMethod("process", ProviderResultRequest.class)
                .getAnnotation(Transactional.class));
    }

    private void assertInvalidCredit(WalletCreditResponse response) {
        ProviderResultRequest request = request(new BigDecimal("50.00"));
        when(sessionValidator.validate(request)).thenReturn(validated(new BigDecimal("50.00")));
        when(walletClient.credit(any(WalletCreditRequest.class))).thenReturn(response);

        WalletClientException exception = assertThrows(
                WalletClientException.class,
                () -> resultService.process(request)
        );
        assertEquals(WalletClientException.Category.INVALID_RESPONSE, exception.getCategory());
    }

    private ProviderResultRequest request(BigDecimal amount) {
        return new ProviderResultRequest(
                PLAYER_ID, "NOVA_SEVEN", "provider-round-id", amount,
                "provider-result-reference", "NOVA_REELS", 1780000001000L,
                "spin", "WEB", RAW_TOKEN, null, null, null, null, null
        );
    }

    private ValidatedProviderResult validated(BigDecimal amount) {
        return new ValidatedProviderResult(
                LOCAL_SESSION_ID, PLAYER_ID, "EUR", "NOVA_REELS", "NOVA_SEVEN",
                "provider-session-id", "provider-round-id", "provider-result-reference", amount
        );
    }

    private WalletCreditResponse creditResponse(boolean duplicate) {
        return new WalletCreditResponse(
                TRANSACTION_ID, PLAYER_ID, "EUR", amount("50.00"), amount("100.00"),
                amount("150.00"), amount("0.00"), "provider-result-reference", duplicate
        );
    }

    private WalletBalanceResponse balanceResponse() {
        return new WalletBalanceResponse(PLAYER_ID, "EUR", amount("100.00"), amount("0.00"));
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
