package com.boki0.casino.wallet.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletTransactionTest {

    @Test
    void constructorStoresRequiredPersistenceValuesAndExternalReference() {
        UUID walletId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        WalletTransaction transaction = new WalletTransaction(
                walletId,
                playerId,
                WalletTransactionType.DEBIT,
                WalletTransactionStatus.COMPLETED,
                new BigDecimal("20.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("980.00"),
                "EUR",
                WalletReferenceType.GAME_BET,
                "provider-bet-reference",
                " provider-bet-reference "
        );

        assertEquals(walletId, transaction.getWalletId());
        assertEquals(playerId, transaction.getAuthUserId());
        assertEquals(WalletTransactionType.DEBIT, transaction.getType());
        assertEquals(WalletTransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals(new BigDecimal("20.00"), transaction.getAmount());
        assertEquals("EUR", transaction.getCurrency());
        assertEquals(new BigDecimal("980.00"), transaction.getBalanceAfter());
        assertEquals("provider-bet-reference", transaction.getExternalReference());
    }

    @Test
    void constructorRejectsBlankExternalReference() {
        assertThrows(
                IllegalArgumentException.class,
                () -> transactionWithExternalReference(" ")
        );
    }

    @Test
    void constructorRejectsMissingRequiredValues() {
        assertThrows(
                NullPointerException.class,
                () -> new WalletTransaction(
                        null,
                        UUID.randomUUID(),
                        WalletTransactionType.DEBIT,
                        WalletTransactionStatus.COMPLETED,
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        new BigDecimal("9.00"),
                        "EUR",
                        WalletReferenceType.GAME_BET,
                        "bet-1",
                        "bet-1"
                )
        );
    }

    private WalletTransaction transactionWithExternalReference(String externalReference) {
        return new WalletTransaction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                WalletTransactionType.DEBIT,
                WalletTransactionStatus.COMPLETED,
                BigDecimal.ONE,
                BigDecimal.TEN,
                new BigDecimal("9.00"),
                "EUR",
                WalletReferenceType.GAME_BET,
                "bet-1",
                externalReference
        );
    }
}
