package com.boki0.casino.wallet.service;

import com.boki0.casino.wallet.dto.CreateWalletRequest;
import com.boki0.casino.wallet.dto.CreditWalletRequest;
import com.boki0.casino.wallet.dto.DebitWalletRequest;
import com.boki0.casino.wallet.dto.WalletResponse;
import com.boki0.casino.wallet.dto.WalletTransactionResponse;
import com.boki0.casino.wallet.entity.Wallet;
import com.boki0.casino.wallet.entity.WalletReferenceType;
import com.boki0.casino.wallet.entity.WalletStatus;
import com.boki0.casino.wallet.entity.WalletTransaction;
import com.boki0.casino.wallet.entity.WalletTransactionStatus;
import com.boki0.casino.wallet.entity.WalletTransactionType;
import com.boki0.casino.wallet.repository.WalletRepository;
import com.boki0.casino.wallet.repository.WalletTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);
    private static final String DEFAULT_CURRENCY = "CREDITS";

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public WalletResponse createWalletForUser(CreateWalletRequest request) {
        validateCreateWalletRequest(request);

        Optional<Wallet> existingWallet = walletRepository.findByAuthUserId(request.authUserId());
        if (existingWallet.isPresent()) {
            return toWalletResponse(existingWallet.get());
        }

        Wallet wallet = new Wallet(request.authUserId(), BigDecimal.ZERO);
        wallet.setCurrency(resolveCurrency(request.currency()));
        wallet.setStatus(WalletStatus.ACTIVE);

        Wallet savedWallet = walletRepository.save(wallet);
        LOGGER.info("Created wallet id={} authUserId={}", savedWallet.getId(), savedWallet.getAuthUserId());

        return toWalletResponse(savedWallet);
    }

    public WalletResponse getWalletByAuthUserId(UUID authUserId) {
        if (authUserId == null) {
            throw new IllegalArgumentException("authUserId must not be null");
        }

        Wallet wallet = walletRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for authUserId: " + authUserId));

        return toWalletResponse(wallet);
    }

    @Transactional
    public WalletTransactionResponse credit(CreditWalletRequest request) {
        validateCreditRequest(request);

        Optional<WalletTransaction> existingTransaction = findExistingTransaction(
                request.idempotencyKey(),
                request.referenceType(),
                request.referenceId()
        );
        if (existingTransaction.isPresent()) {
            return toWalletTransactionResponse(existingTransaction.get());
        }

        Wallet wallet = findWalletForUpdate(request.authUserId());
        ensureWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.amount());
        wallet.setBalance(balanceAfter);

        WalletTransaction transaction = buildTransaction(
                wallet,
                WalletTransactionType.CREDIT,
                WalletTransactionStatus.COMPLETED,
                request.amount(),
                balanceBefore,
                balanceAfter,
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey(),
                request.description()
        );

        walletRepository.save(wallet);
        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

        LOGGER.info(
                "Credited wallet id={} authUserId={} amount={} balanceAfter={}",
                wallet.getId(),
                wallet.getAuthUserId(),
                request.amount(),
                balanceAfter
        );

        return toWalletTransactionResponse(savedTransaction);
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public WalletTransactionResponse debit(DebitWalletRequest request) {
        validateDebitRequest(request);

        Optional<WalletTransaction> existingTransaction = findExistingTransaction(
                request.idempotencyKey(),
                request.referenceType(),
                request.referenceId()
        );
        if (existingTransaction.isPresent()) {
            return toWalletTransactionResponse(existingTransaction.get());
        }

        Wallet wallet = findWalletForUpdate(request.authUserId());
        ensureWalletActive(wallet);

        BigDecimal balanceBefore = wallet.getBalance();
        if (balanceBefore.compareTo(request.amount()) < 0) {
            WalletTransaction rejectedTransaction = buildTransaction(
                    wallet,
                    WalletTransactionType.DEBIT,
                    WalletTransactionStatus.REJECTED,
                    request.amount(),
                    balanceBefore,
                    balanceBefore,
                    request.referenceType(),
                    request.referenceId(),
                    request.idempotencyKey(),
                    buildInsufficientFundsDescription(request.description())
            );

            walletTransactionRepository.save(rejectedTransaction);
            LOGGER.info(
                    "Rejected debit wallet id={} authUserId={} amount={} reason=Insufficient funds",
                    wallet.getId(),
                    wallet.getAuthUserId(),
                    request.amount()
            );
            throw new IllegalArgumentException("Insufficient funds");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(request.amount());
        wallet.setBalance(balanceAfter);

        WalletTransaction transaction = buildTransaction(
                wallet,
                WalletTransactionType.DEBIT,
                WalletTransactionStatus.COMPLETED,
                request.amount(),
                balanceBefore,
                balanceAfter,
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey(),
                request.description()
        );

        walletRepository.save(wallet);
        WalletTransaction savedTransaction = walletTransactionRepository.save(transaction);

        LOGGER.info(
                "Debited wallet id={} authUserId={} amount={} balanceAfter={}",
                wallet.getId(),
                wallet.getAuthUserId(),
                request.amount(),
                balanceAfter
        );

        return toWalletTransactionResponse(savedTransaction);
    }

    private void validateCreateWalletRequest(CreateWalletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreateWalletRequest must not be null");
        }
        if (request.authUserId() == null) {
            throw new IllegalArgumentException("authUserId must not be null");
        }
    }

    private void validateCreditRequest(CreditWalletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("CreditWalletRequest must not be null");
        }
        validateCommonTransactionRequest(
                request.authUserId(),
                request.amount(),
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey()
        );
    }

    private void validateDebitRequest(DebitWalletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("DebitWalletRequest must not be null");
        }
        validateCommonTransactionRequest(
                request.authUserId(),
                request.amount(),
                request.referenceType(),
                request.referenceId(),
                request.idempotencyKey()
        );
    }

    private void validateCommonTransactionRequest(
            UUID authUserId,
            BigDecimal amount,
            WalletReferenceType referenceType,
            String referenceId,
            String idempotencyKey
    ) {
        if (authUserId == null) {
            throw new IllegalArgumentException("authUserId must not be null");
        }
        validateAmount(amount);
        if (referenceType == null) {
            throw new IllegalArgumentException("referenceType must not be null");
        }
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("referenceId must not be blank");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("idempotencyKey must not be blank");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private void ensureWalletActive(Wallet wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException("Wallet is not active");
        }
    }

    private Wallet findWalletForUpdate(UUID authUserId) {
        return walletRepository.findByAuthUserIdForUpdate(authUserId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for authUserId: " + authUserId));
    }

    private Optional<WalletTransaction> findExistingTransaction(
            String idempotencyKey,
            WalletReferenceType referenceType,
            String referenceId
    ) {
        Optional<WalletTransaction> transactionByIdempotencyKey =
                walletTransactionRepository.findByIdempotencyKey(idempotencyKey);
        if (transactionByIdempotencyKey.isPresent()) {
            return transactionByIdempotencyKey;
        }

        return walletTransactionRepository.findByReferenceTypeAndReferenceId(referenceType, referenceId);
    }

    private WalletTransaction buildTransaction(
            Wallet wallet,
            WalletTransactionType type,
            WalletTransactionStatus status,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            WalletReferenceType referenceType,
            String referenceId,
            String idempotencyKey,
            String description
    ) {
        WalletTransaction transaction = new WalletTransaction(
                wallet.getId(),
                wallet.getAuthUserId(),
                type,
                status,
                amount,
                balanceBefore,
                balanceAfter,
                wallet.getCurrency(),
                referenceType,
                referenceId,
                idempotencyKey
        );
        transaction.setDescription(description);
        return transaction;
    }

    private String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        return currency;
    }

    private String buildInsufficientFundsDescription(String description) {
        if (description == null || description.isBlank()) {
            return "Insufficient funds";
        }
        return description + " - Insufficient funds";
    }

    private WalletResponse toWalletResponse(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getAuthUserId(),
                wallet.getBalance(),
                wallet.getCurrency(),
                wallet.getStatus(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    private WalletTransactionResponse toWalletTransactionResponse(WalletTransaction transaction) {
        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getAuthUserId(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getAmount(),
                transaction.getBalanceBefore(),
                transaction.getBalanceAfter(),
                transaction.getCurrency(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getIdempotencyKey(),
                transaction.getDescription(),
                transaction.getCreatedAt()
        );
    }
}
