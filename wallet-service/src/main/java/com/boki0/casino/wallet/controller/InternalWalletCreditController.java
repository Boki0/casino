package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.dto.InternalWalletCreditRequest;
import com.boki0.casino.wallet.dto.InternalWalletCreditResponse;
import com.boki0.casino.wallet.service.WalletCreditCommand;
import com.boki0.casino.wallet.service.WalletCreditResult;
import com.boki0.casino.wallet.service.WalletCreditService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/wallets")
public class InternalWalletCreditController {

    private final WalletCreditService walletCreditService;

    public InternalWalletCreditController(WalletCreditService walletCreditService) {
        this.walletCreditService = walletCreditService;
    }

    @PostMapping(
            path = "/credit",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public InternalWalletCreditResponse credit(@Valid @RequestBody InternalWalletCreditRequest request) {
        WalletCreditResult result = walletCreditService.credit(new WalletCreditCommand(
                request.playerId(),
                request.currency(),
                request.amount(),
                request.reference()
        ));
        return InternalWalletCreditResponse.from(result);
    }
}
