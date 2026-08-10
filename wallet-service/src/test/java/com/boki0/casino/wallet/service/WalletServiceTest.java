package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.dto.InternalWalletBalanceResponse;
import com.boki0.casino.wallet.entity.Wallet;
import com.boki0.casino.wallet.exception.WalletNotFoundException;
import com.boki0.casino.wallet.repository.WalletRepository;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalletServiceTest {

    private WalletRepositoryFake walletRepositoryFake;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepositoryFake = new WalletRepositoryFake();
        WalletRepository walletRepository = walletRepositoryFake.repository();
        WalletTransactionRepository walletTransactionRepository = emptyTransactionRepository();
        walletService = new WalletService(walletRepository, walletTransactionRepository);
    }

    @Test
    void getInternalBalance_returnsCashAndZeroBonusWithoutMutatingWallet() {
        UUID playerId = UUID.randomUUID();
        Wallet wallet = wallet(playerId, new BigDecimal("125.50"), "EUR");
        walletRepositoryFake.result = Optional.of(wallet);

        InternalWalletBalanceResponse response =
                walletService.getInternalBalance(playerId, "eur");

        assertEquals(playerId, response.playerId());
        assertEquals("EUR", response.currency());
        assertEquals(new BigDecimal("125.50"), response.cash());
        assertEquals(BigDecimal.ZERO, response.bonus());
        assertEquals(new BigDecimal("125.50"), wallet.getBalance());
        assertEquals(0, walletRepositoryFake.saveCallCount);
        assertEquals(playerId, walletRepositoryFake.requestedPlayerId);
        assertEquals("EUR", walletRepositoryFake.requestedCurrency);
    }

    @Test
    void getInternalBalance_returnsRealZeroBalance() {
        UUID playerId = UUID.randomUUID();
        Wallet wallet = wallet(playerId, BigDecimal.ZERO, "EUR");
        walletRepositoryFake.result = Optional.of(wallet);

        InternalWalletBalanceResponse response =
                walletService.getInternalBalance(playerId, " EUR ");

        assertEquals(BigDecimal.ZERO, response.cash());
        assertEquals(BigDecimal.ZERO, response.bonus());
    }

    @Test
    void getInternalBalance_missingWalletThrowsWalletNotFound() {
        UUID playerId = UUID.randomUUID();

        assertThrows(
                WalletNotFoundException.class,
                () -> walletService.getInternalBalance(playerId, "usd")
        );
    }

    @Test
    void getInternalBalance_wrongCurrencyIsTreatedAsMissingWallet() {
        UUID playerId = UUID.randomUUID();

        assertThrows(
                WalletNotFoundException.class,
                () -> walletService.getInternalBalance(playerId, "USD")
        );
    }

    @Test
    void getInternalBalance_blankCurrencyIsRejected() {
        UUID playerId = UUID.randomUUID();

        assertThrows(
                IllegalArgumentException.class,
                () -> walletService.getInternalBalance(playerId, " ")
        );
    }

    private Wallet wallet(UUID playerId, BigDecimal balance, String currency) {
        Wallet wallet = new Wallet(playerId, balance);
        wallet.setCurrency(currency);
        return wallet;
    }

    private WalletTransactionRepository emptyTransactionRepository() {
        return (WalletTransactionRepository) Proxy.newProxyInstance(
                WalletTransactionRepository.class.getClassLoader(),
                new Class<?>[]{WalletTransactionRepository.class},
                (proxy, method, arguments) -> defaultValue(method.getReturnType())
        );
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return 0;
    }

    private static class WalletRepositoryFake {
        private Optional<Wallet> result = Optional.empty();
        private UUID requestedPlayerId;
        private String requestedCurrency;
        private int saveCallCount;

        WalletRepository repository() {
            return (WalletRepository) Proxy.newProxyInstance(
                    WalletRepository.class.getClassLoader(),
                    new Class<?>[]{WalletRepository.class},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("findByAuthUserIdAndCurrency")) {
                            requestedPlayerId = (UUID) arguments[0];
                            requestedCurrency = (String) arguments[1];
                            return result;
                        }
                        if (method.getName().equals("save")) {
                            saveCallCount++;
                            return arguments[0];
                        }
                        return defaultValue(method.getReturnType());
                    }
            );
        }
    }
}
