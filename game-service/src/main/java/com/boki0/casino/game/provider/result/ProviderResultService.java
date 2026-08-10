package com.boki0.casino.game.provider.result;

import com.boki0.casino.game.provider.auth.ProviderResultSessionValidator;
import com.boki0.casino.game.provider.auth.ValidatedProviderResult;
import com.boki0.casino.game.provider.dto.ProviderResultRequest;
import com.boki0.casino.game.provider.dto.ProviderResultResponse;
import com.boki0.casino.game.wallet.client.WalletBalanceClient;
import com.boki0.casino.game.wallet.dto.WalletBalanceResponse;
import com.boki0.casino.game.wallet.dto.WalletCreditRequest;
import com.boki0.casino.game.wallet.dto.WalletCreditResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProviderResultService {

    private final ProviderResultSessionValidator sessionValidator;
    private final WalletBalanceClient walletClient;

    public ProviderResultService(
            ProviderResultSessionValidator sessionValidator,
            WalletBalanceClient walletClient
    ) {
        this.sessionValidator = sessionValidator;
        this.walletClient = walletClient;
    }

    public ProviderResultResponse process(ProviderResultRequest request) {
        ValidatedProviderResult validatedResult = sessionValidator.validate(request);
        if (validatedResult.amount().compareTo(BigDecimal.ZERO) == 0) {
            return processZeroResult(validatedResult);
        }

        WalletCreditResponse creditResponse = walletClient.credit(new WalletCreditRequest(
                validatedResult.playerId(),
                validatedResult.currency(),
                validatedResult.amount(),
                validatedResult.reference()
        ));
        validateCreditResponse(creditResponse, validatedResult);

        return new ProviderResultResponse(
                creditResponse.transactionId(),
                creditResponse.currency(),
                creditResponse.cash(),
                creditResponse.bonus(),
                0,
                "Success"
        );
    }

    private ProviderResultResponse processZeroResult(ValidatedProviderResult validatedResult) {
        WalletBalanceResponse balanceResponse = walletClient.getBalance(
                validatedResult.playerId(),
                validatedResult.currency()
        );
        validateBalanceResponse(balanceResponse, validatedResult);

        return new ProviderResultResponse(
                validatedResult.localSessionId(),
                balanceResponse.currency(),
                balanceResponse.cash(),
                balanceResponse.bonus(),
                0,
                "Success"
        );
    }

    private void validateCreditResponse(
            WalletCreditResponse response,
            ValidatedProviderResult validatedResult
    ) {
        if (response == null
                || response.transactionId() == null
                || response.playerId() == null
                || response.currency() == null
                || response.amount() == null
                || response.cash() == null
                || response.bonus() == null
                || response.reference() == null
                || !validatedResult.playerId().equals(response.playerId())
                || !validatedResult.currency().equals(response.currency())
                || validatedResult.amount().compareTo(response.amount()) != 0
                || !validatedResult.reference().equals(response.reference())) {
            throw invalidResponse("credit");
        }
    }

    private void validateBalanceResponse(
            WalletBalanceResponse response,
            ValidatedProviderResult validatedResult
    ) {
        if (response == null
                || response.playerId() == null
                || response.currency() == null
                || response.cash() == null
                || response.bonus() == null
                || !validatedResult.playerId().equals(response.playerId())
                || !validatedResult.currency().equals(response.currency())) {
            throw invalidResponse("balance");
        }
    }

    private WalletClientException invalidResponse(String operation) {
        return new WalletClientException(
                "Wallet service returned an invalid " + operation + " response",
                WalletClientException.Category.INVALID_RESPONSE
        );
    }
}
