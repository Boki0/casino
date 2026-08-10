package com.boki0.casino.wallet.entity;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletTransactionTest {

    @Test
    void completedCreditStoresExistingTransactionModelValues() {
        UUID walletId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();

        WalletTransaction transaction = new WalletTransaction(
                walletId,
                playerId,
                WalletTransactionType.CREDIT,
                WalletTransactionStatus.COMPLETED,
                new BigDecimal("25.00"),
                new BigDecimal("100.00"),
                new BigDecimal("125.00"),
                "EUR",
                WalletReferenceType.PAYMENT,
                "provider-result-reference",
                "provider-result-reference"
        );
        transaction.prePersist();

        assertEquals(WalletTransactionType.CREDIT, transaction.getType());
        assertEquals(WalletTransactionStatus.COMPLETED, transaction.getStatus());
        assertEquals(new BigDecimal("25.00"), transaction.getAmount());
        assertEquals(new BigDecimal("100.00"), transaction.getBalanceBefore());
        assertEquals(new BigDecimal("125.00"), transaction.getBalanceAfter());
        assertEquals("EUR", transaction.getCurrency());
        assertEquals("provider-result-reference", transaction.getExternalReference());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void creditTypeUsesExistingStringEnumPersistence() throws Exception {
        assertEquals(WalletTransactionType.CREDIT, WalletTransactionType.valueOf("CREDIT"));

        Field typeField = WalletTransaction.class.getDeclaredField("type");
        Enumerated enumerated = typeField.getAnnotation(Enumerated.class);

        assertNotNull(enumerated);
        assertEquals(EnumType.STRING, enumerated.value());
    }

    @Test
    void externalReferenceIdempotencyRemainsGloballyUnique() {
        Table table = WalletTransaction.class.getAnnotation(Table.class);

        assertTrue(Arrays.stream(table.uniqueConstraints())
                .anyMatch(constraint -> constraint.name().equals(
                        "uk_wallet_transactions_idempotency_key"
                ) && Arrays.equals(constraint.columnNames(), new String[]{"idempotency_key"})));
    }

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
