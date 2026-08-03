package com.boki0.casino.game.provider.bet;

import com.boki0.casino.game.provider.auth.ProviderBetSessionValidator;
import com.boki0.casino.game.provider.auth.ValidatedProviderBet;
import com.boki0.casino.game.provider.dto.ProviderBetRequest;
import com.boki0.casino.game.provider.dto.ProviderBetResponse;
import com.boki0.casino.game.wallet.client.WalletBalanceClient;
import com.boki0.casino.game.wallet.dto.WalletDebitRequest;
import com.boki0.casino.game.wallet.dto.WalletDebitResponse;
import com.boki0.casino.game.wallet.exception.WalletClientException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProviderBetService {

    private final ProviderBetSessionValidator sessionValidator;
    private final WalletBalanceClient walletClient;

    public ProviderBetService(
            ProviderBetSessionValidator sessionValidator,
            WalletBalanceClient walletClient
    ) {
        this.sessionValidator = sessionValidator;
        this.walletClient = walletClient;
    }

    public ProviderBetResponse process(ProviderBetRequest request) {
        ValidatedProviderBet validatedBet = sessionValidator.validate(request);
        WalletDebitResponse debitResponse = walletClient.debit(new WalletDebitRequest(
                validatedBet.playerId(),
                validatedBet.currency(),
                validatedBet.amount(),
                validatedBet.reference()
        ));
        validateDebitResponse(debitResponse, validatedBet);

        return new ProviderBetResponse(
                debitResponse.transactionId(),
                debitResponse.currency(),
                debitResponse.cash(),
                debitResponse.bonus(),
                BigDecimal.ZERO,
                0,
                "Success"
        );
    }

    private void validateDebitResponse(
            WalletDebitResponse response,
            ValidatedProviderBet validatedBet
    ) {
        if (response == null
                || response.transactionId() == null
                || response.playerId() == null
                || response.currency() == null
                || response.amount() == null
                || response.cash() == null
                || response.bonus() == null
                || response.reference() == null
                || !validatedBet.playerId().equals(response.playerId())
                || !validatedBet.currency().equals(response.currency())
                || validatedBet.amount().compareTo(response.amount()) != 0
                || !validatedBet.reference().equals(response.reference())) {
            throw new WalletClientException(
                    "Wallet service returned an invalid debit response",
                    WalletClientException.Category.INVALID_RESPONSE
            );
        }
    }
}
