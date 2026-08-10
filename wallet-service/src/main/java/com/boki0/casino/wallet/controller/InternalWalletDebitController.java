package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.dto.InternalWalletDebitRequest;
import com.boki0.casino.wallet.dto.InternalWalletDebitResponse;
import com.boki0.casino.wallet.service.WalletDebitCommand;
import com.boki0.casino.wallet.service.WalletDebitResult;
import com.boki0.casino.wallet.service.WalletDebitService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/wallets")
public class InternalWalletDebitController {

    private final WalletDebitService walletDebitService;

    public InternalWalletDebitController(WalletDebitService walletDebitService) {
        this.walletDebitService = walletDebitService;
    }

    @PostMapping(
            path = "/debit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public InternalWalletDebitResponse debit(@Valid @RequestBody InternalWalletDebitRequest request) {
        WalletDebitResult result = walletDebitService.debit(new WalletDebitCommand(
                request.playerId(),
                request.currency(),
                request.amount(),
                request.reference()
        ));
        return InternalWalletDebitResponse.from(result);
    }
}
