package com.boki0.casino.game.provider.auth;

import com.boki0.casino.game.provider.dto.ProviderAuthenticateRequest;
import com.boki0.casino.game.provider.dto.ProviderAuthenticateResponse;
import com.boki0.casino.game.provider.exception.ProviderAuthenticationException;
import com.boki0.casino.game.service.GameSessionTokenService;
import com.boki0.casino.game.wallet.client.WalletBalanceClient;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class ProviderSessionAuthenticationService {

    private final GameSessionTokenService tokenService;
    private final ProviderSessionValidationService validationService;
    private final WalletBalanceClient walletBalanceClient;

    public ProviderSessionAuthenticationService(
            GameSessionTokenService tokenService,
            ProviderSessionValidationService validationService,
            WalletBalanceClient walletBalanceClient
    ) {
        this.tokenService = tokenService;
        this.validationService = validationService;
        this.walletBalanceClient = walletBalanceClient;
    }

    public ProviderAuthenticateResponse authenticate(ProviderAuthenticateRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        String tokenHash = tokenService.hashToken(request.token());
        ValidatedProviderSession session = validationService.validate(
                tokenHash,
                request.providerCode(),
                request.gameCode(),
                request.sessionId()
        );

        WalletBalanceResponse wallet = getWalletBalance(session);
        if (!session.currency().equals(wallet.currency())) {
            throw ProviderAuthenticationException.walletCurrencyMismatch();
        }

        return ProviderAuthenticateResponse.success(
                session.playerId(),
                session.currency(),
                wallet.balance()
        );
    }

    private WalletBalanceResponse getWalletBalance(ValidatedProviderSession session) {
        try {
            return walletBalanceClient.getBalance(session.playerId());
        } catch (WalletClientException exception) {
            throw ProviderAuthenticationException.walletUnavailable();
        }
    }
}
