package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.Wallet;
import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.entity.WalletTransaction;
import com.boki0.casino.wallet.entity.WalletTransactionStatus;
import com.boki0.casino.wallet.entity.WalletTransactionType;
import com.boki0.casino.wallet.exception.WalletNotFoundException;
import com.boki0.casino.wallet.exception.WalletTransactionIdempotencyConflictException;
import com.boki0.casino.wallet.repository.WalletRepository;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletCreditServiceTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    private Wallet wallet;
    private WalletRepositoryFake walletRepositoryFake;
    private WalletTransactionRepositoryFake transactionRepositoryFake;
    private WalletCreditTransactionalExecutor executor;

    @BeforeEach
    void setUp() {
        wallet = new Wallet(PLAYER_ID, new BigDecimal("100.00"));
        wallet.setId(UUID.randomUUID());
        wallet.setCurrency("EUR");
        walletRepositoryFake = new WalletRepositoryFake(wallet);
        transactionRepositoryFake = new WalletTransactionRepositoryFake();
        executor = new WalletCreditTransactionalExecutor(
                walletRepositoryFake.repository(),
                transactionRepositoryFake.repository(),
                new WalletCreditTransactionResolver()
        );
    }

    @Test
    void successfulCreditIncreasesBalanceAndPersistsTransaction() {
        WalletCreditResult result = executor.creditOnce(command("20.00", "win-1"));

        assertEquals(new BigDecimal("120.00"), wallet.getBalance());
        assertEquals(new BigDecimal("100.00"), result.balanceBefore());
        assertEquals(new BigDecimal("120.00"), result.balanceAfter());
        assertEquals(new BigDecimal("20.00"), result.amount());
        assertEquals("win-1", result.externalReference());
        assertFalse(result.duplicate());
        assertEquals(1, transactionRepositoryFake.saveCount);
        assertEquals(WalletTransactionType.CREDIT, transactionRepositoryFake.transactions.get("win-1").getType());
        assertEquals(WalletReferenceType.GAME_WIN, transactionRepositoryFake.transactions.get("win-1").getReferenceType());
    }

    @Test
    void commandNormalizesCurrencyAndTrimsReference() {
        WalletCreditCommand command = new WalletCreditCommand(
                PLAYER_ID, " eur ", new BigDecimal("10.00"), " win-1 "
        );

        assertEquals("EUR", command.currency());
        assertEquals("win-1", command.externalReference());
    }

    @Test
    void identicalDuplicateReturnsOriginalResultWithoutSecondCredit() {
        WalletCreditResult first = executor.creditOnce(command("20.00", "win-1"));
        WalletCreditResult duplicate = executor.creditOnce(command("20.0", "win-1"));

        assertEquals(first.transactionId(), duplicate.transactionId());
        assertEquals(first.balanceBefore(), duplicate.balanceBefore());
        assertEquals(first.balanceAfter(), duplicate.balanceAfter());
        assertEquals(new BigDecimal("120.00"), wallet.getBalance());
        assertTrue(duplicate.duplicate());
        assertEquals(1, transactionRepositoryFake.saveCount);
    }

    @Test
    void reusedReferenceWithDifferentAmountPlayerOrCurrencyIsRejected() {
        executor.creditOnce(command("20.00", "win-1"));

        assertConflict(new WalletCreditCommand(PLAYER_ID, "EUR", new BigDecimal("30.00"), "win-1"));
        assertConflict(new WalletCreditCommand(UUID.randomUUID(), "EUR", new BigDecimal("20.00"), "win-1"));
        assertConflict(new WalletCreditCommand(PLAYER_ID, "USD", new BigDecimal("20.00"), "win-1"));
        assertEquals(new BigDecimal("120.00"), wallet.getBalance());
        assertEquals(1, transactionRepositoryFake.saveCount);
    }

    @Test
    void referenceAlreadyUsedByDebitIsRejected() {
        WalletCreditCommand command = command("20.00", "shared-ref");
        transactionRepositoryFake.transactions.put("shared-ref", transaction(
                command, WalletTransactionType.DEBIT, new BigDecimal("80.00")
        ));

        assertThrows(
                WalletTransactionIdempotencyConflictException.class,
                () -> executor.creditOnce(command)
        );
        assertEquals(new BigDecimal("100.00"), wallet.getBalance());
    }

    @Test
    void invalidAmountsAreRejectedByCommand() {
        assertThrows(
                NullPointerException.class,
                () -> new WalletCreditCommand(PLAYER_ID, "EUR", null, "win-null")
        );
        assertThrows(IllegalArgumentException.class, () -> command("0.00", "win-zero"));
        assertThrows(IllegalArgumentException.class, () -> command("-1.00", "win-negative"));
    }

    @Test
    void missingWalletIsRejected() {
        walletRepositoryFake.wallet = null;

        assertThrows(WalletNotFoundException.class, () -> executor.creditOnce(command("20.00", "win-1")));
        assertEquals(0, transactionRepositoryFake.saveCount);
    }

    @Test
    void differentReferencesCreditSequentiallyWithoutLostUpdates() {
        executor.creditOnce(command("20.00", "win-1"));
        WalletCreditResult second = executor.creditOnce(command("15.00", "win-2"));

        assertEquals(new BigDecimal("135.00"), wallet.getBalance());
        assertEquals(new BigDecimal("120.00"), second.balanceBefore());
        assertEquals(new BigDecimal("135.00"), second.balanceAfter());
        assertEquals(2, transactionRepositoryFake.saveCount);
    }

    @Test
    void uniqueConstraintRaceIsResolvedAfterRollbackUsingFreshLookup() {
        WalletCreditCommand command = command("20.00", "win-race");
        WalletTransaction stored = transaction(command, WalletTransactionType.CREDIT, new BigDecimal("120.00"));
        WalletCreditTransactionalExecutor failingExecutor = new WalletCreditTransactionalExecutor(null, null, null) {
            @Override
            public WalletCreditResult creditOnce(WalletCreditCommand ignored) {
                throw new DataIntegrityViolationException("duplicate external reference");
            }
        };
        WalletCreditTransactionLookupService lookupService = new WalletCreditTransactionLookupService(null) {
            @Override
            public Optional<WalletTransaction> findByExternalReference(String ignored) {
                return Optional.of(stored);
            }
        };
        WalletCreditService service = new WalletCreditService(
                failingExecutor, lookupService, new WalletCreditTransactionResolver()
        );

        WalletCreditResult result = service.credit(command);

        assertEquals(stored.getId(), result.transactionId());
        assertEquals(new BigDecimal("100.00"), result.balanceBefore());
        assertEquals(new BigDecimal("120.00"), result.balanceAfter());
        assertTrue(result.duplicate());
    }

    private void assertConflict(WalletCreditCommand command) {
        assertThrows(WalletTransactionIdempotencyConflictException.class, () -> executor.creditOnce(command));
    }

    private WalletCreditCommand command(String amount, String reference) {
        return new WalletCreditCommand(PLAYER_ID, "eur", new BigDecimal(amount), reference);
    }

    private WalletTransaction transaction(
            WalletCreditCommand command,
            WalletTransactionType type,
            BigDecimal balanceAfter
    ) {
        WalletTransaction transaction = new WalletTransaction(
                wallet.getId(), command.playerId(), type, WalletTransactionStatus.COMPLETED,
                command.amount(),
                type == WalletTransactionType.CREDIT
                        ? balanceAfter.subtract(command.amount())
                        : balanceAfter.add(command.amount()),
                balanceAfter, command.currency(),
                type == WalletTransactionType.CREDIT ? WalletReferenceType.GAME_WIN : WalletReferenceType.GAME_BET,
                command.externalReference(), command.externalReference()
        );
        transaction.setId(UUID.randomUUID());
        return transaction;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Optional.class) return Optional.empty();
        if (!returnType.isPrimitive()) return null;
        if (returnType == boolean.class) return false;
        return 0;
    }

    private static class WalletRepositoryFake {
        private Wallet wallet;

        WalletRepositoryFake(Wallet wallet) {
            this.wallet = wallet;
        }

        WalletRepository repository() {
            return (WalletRepository) Proxy.newProxyInstance(
                    WalletRepository.class.getClassLoader(), new Class<?>[]{WalletRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("findByAuthUserIdForUpdate")) return Optional.ofNullable(wallet);
                        if (method.getName().equals("save")) return arguments[0];
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }

    private static class WalletTransactionRepositoryFake {
        private final Map<String, WalletTransaction> transactions = new LinkedHashMap<>();
        private int saveCount;

        WalletTransactionRepository repository() {
            return (WalletTransactionRepository) Proxy.newProxyInstance(
                    WalletTransactionRepository.class.getClassLoader(),
                    new Class<?>[]{WalletTransactionRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("findByExternalReference")
                                || method.getName().equals("findByIdempotencyKey")) {
                            return Optional.ofNullable(transactions.get(arguments[0]));
                        }
                        if (method.getName().equals("saveAndFlush")) {
                            WalletTransaction transaction = (WalletTransaction) arguments[0];
                            transaction.setId(UUID.randomUUID());
                            transactions.put(transaction.getExternalReference(), transaction);
                            saveCount++;
                            return transaction;
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }
}
