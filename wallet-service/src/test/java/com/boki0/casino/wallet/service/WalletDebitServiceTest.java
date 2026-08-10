package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.entity.Wallet;
import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.entity.WalletTransaction;
import com.boki0.casino.wallet.entity.WalletTransactionStatus;
import com.boki0.casino.wallet.entity.WalletTransactionType;
import com.boki0.casino.wallet.exception.InsufficientWalletBalanceException;
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

class WalletDebitServiceTest {

    private static final UUID PLAYER_ID = UUID.randomUUID();

    private Wallet wallet;
    private WalletRepositoryFake walletRepositoryFake;
    private WalletTransactionRepositoryFake transactionRepositoryFake;
    private WalletDebitTransactionalExecutor executor;

    @BeforeEach
    void setUp() {
        wallet = new Wallet(PLAYER_ID, new BigDecimal("100.00"));
        wallet.setId(UUID.randomUUID());
        wallet.setCurrency("EUR");
        walletRepositoryFake = new WalletRepositoryFake(wallet);
        transactionRepositoryFake = new WalletTransactionRepositoryFake();
        executor = new WalletDebitTransactionalExecutor(
                walletRepositoryFake.repository(),
                transactionRepositoryFake.repository(),
                new WalletDebitTransactionResolver()
        );
    }

    @Test
    void successfulDebitReducesBalanceAndPersistsTransaction() {
        WalletDebitResult result = executor.debitOnce(command("20.00", "bet-1"));

        assertEquals(new BigDecimal("80.00"), wallet.getBalance());
        assertEquals(new BigDecimal("80.00"), result.balanceAfter());
        assertEquals(new BigDecimal("20.00"), result.amount());
        assertEquals("bet-1", result.externalReference());
        assertFalse(result.duplicate());
        assertEquals(1, transactionRepositoryFake.saveCount);
        assertEquals(result.transactionId(), transactionRepositoryFake.transactions.get("bet-1").getId());
    }

    @Test
    void identicalDuplicateReturnsOriginalResultWithoutSecondDebit() {
        WalletDebitResult first = executor.debitOnce(command("20.00", "bet-1"));
        WalletDebitResult duplicate = executor.debitOnce(command("20.0", "bet-1"));

        assertEquals(first.transactionId(), duplicate.transactionId());
        assertEquals(first.balanceAfter(), duplicate.balanceAfter());
        assertEquals(new BigDecimal("80.00"), wallet.getBalance());
        assertTrue(duplicate.duplicate());
        assertEquals(1, transactionRepositoryFake.saveCount);
    }

    @Test
    void reusedReferenceWithDifferentAmountPlayerOrCurrencyIsRejected() {
        executor.debitOnce(command("20.00", "bet-1"));

        assertThrows(
                WalletTransactionIdempotencyConflictException.class,
                () -> executor.debitOnce(command("30.00", "bet-1"))
        );
        assertThrows(
                WalletTransactionIdempotencyConflictException.class,
                () -> executor.debitOnce(new WalletDebitCommand(
                        UUID.randomUUID(), "EUR", new BigDecimal("20.00"), "bet-1"
                ))
        );
        assertThrows(
                WalletTransactionIdempotencyConflictException.class,
                () -> executor.debitOnce(new WalletDebitCommand(
                        PLAYER_ID, "USD", new BigDecimal("20.00"), "bet-1"
                ))
        );
        assertEquals(new BigDecimal("80.00"), wallet.getBalance());
        assertEquals(1, transactionRepositoryFake.saveCount);
    }

    @Test
    void insufficientBalanceDoesNotMutateWalletOrPersistTransaction() {
        assertThrows(
                InsufficientWalletBalanceException.class,
                () -> executor.debitOnce(command("120.00", "bet-1"))
        );

        assertEquals(new BigDecimal("100.00"), wallet.getBalance());
        assertEquals(0, transactionRepositoryFake.saveCount);
    }

    @Test
    void invalidAmountsAreRejectedByCommand() {
        assertThrows(IllegalArgumentException.class, () -> command("0.00", "bet-zero"));
        assertThrows(IllegalArgumentException.class, () -> command("-1.00", "bet-negative"));
    }

    @Test
    void missingWalletIsRejected() {
        walletRepositoryFake.wallet = null;

        assertThrows(
                WalletNotFoundException.class,
                () -> executor.debitOnce(command("20.00", "bet-1"))
        );
    }

    @Test
    void differentReferencesDebitSequentiallyWithoutLostUpdates() {
        executor.debitOnce(command("20.00", "bet-1"));
        WalletDebitResult second = executor.debitOnce(command("15.00", "bet-2"));

        assertEquals(new BigDecimal("65.00"), wallet.getBalance());
        assertEquals(new BigDecimal("65.00"), second.balanceAfter());
        assertEquals(2, transactionRepositoryFake.saveCount);
    }

    @Test
    void uniqueConstraintRaceIsResolvedAfterRollbackUsingFreshLookup() {
        WalletDebitCommand command = command("20.00", "bet-race");
        WalletTransaction stored = completedTransaction(command, new BigDecimal("80.00"));
        WalletDebitTransactionalExecutor failingExecutor = new WalletDebitTransactionalExecutor(
                null, null, null
        ) {
            @Override
            public WalletDebitResult debitOnce(WalletDebitCommand ignored) {
                throw new DataIntegrityViolationException("duplicate external reference");
            }
        };
        WalletDebitTransactionLookupService lookupService = new WalletDebitTransactionLookupService(null) {
            @Override
            public Optional<WalletTransaction> findByExternalReference(String ignored) {
                return Optional.of(stored);
            }
        };
        WalletDebitService service = new WalletDebitService(
                failingExecutor,
                lookupService,
                new WalletDebitTransactionResolver()
        );

        WalletDebitResult result = service.debit(command);

        assertEquals(stored.getId(), result.transactionId());
        assertEquals(new BigDecimal("80.00"), result.balanceAfter());
        assertTrue(result.duplicate());
    }

    private WalletDebitCommand command(String amount, String reference) {
        return new WalletDebitCommand(PLAYER_ID, "eur", new BigDecimal(amount), reference);
    }

    private WalletTransaction completedTransaction(WalletDebitCommand command, BigDecimal balanceAfter) {
        WalletTransaction transaction = new WalletTransaction(
                wallet.getId(),
                command.playerId(),
                WalletTransactionType.DEBIT,
                WalletTransactionStatus.COMPLETED,
                command.amount(),
                balanceAfter.add(command.amount()),
                balanceAfter,
                command.currency(),
                WalletReferenceType.GAME_BET,
                command.externalReference(),
                command.externalReference()
        );
        transaction.setId(UUID.randomUUID());
        return transaction;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Optional.class) {
            return Optional.empty();
        }
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        return 0;
    }

    private static class WalletRepositoryFake {
        private Wallet wallet;

        WalletRepositoryFake(Wallet wallet) {
            this.wallet = wallet;
        }

        WalletRepository repository() {
            return (WalletRepository) Proxy.newProxyInstance(
                    WalletRepository.class.getClassLoader(),
                    new Class<?>[]{WalletRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("findByAuthUserIdForUpdate")) {
                            return Optional.ofNullable(wallet);
                        }
                        if (method.getName().equals("save")) {
                            return arguments[0];
                        }
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
