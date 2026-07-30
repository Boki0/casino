package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.dto.InternalWalletBalanceResponse;
import com.boki0.casino.wallet.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/wallets")
public class InternalWalletController {

    private final WalletService walletService;

    public InternalWalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public InternalWalletBalanceResponse getBalance(
            @RequestParam UUID playerId,
            @RequestParam String currency
    ) {
        return walletService.getInternalBalance(playerId, currency);
    }
}
