package com.boki0.casino.wallet.controller;

import com.boki0.casino.wallet.dto.WalletResponse;
import com.boki0.casino.wallet.service.WalletService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public WalletResponse getCurrentUserWallet(
            @RequestHeader(value = HEADER_AUTH_USER_ID, required = false) String authUserIdHeader
    ) {
        if (authUserIdHeader == null || authUserIdHeader.isBlank()) {
            throw new IllegalArgumentException("Missing X-Auth-User-Id header");
        }

        UUID authUserId;
        try {
            authUserId = UUID.fromString(authUserIdHeader);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid X-Auth-User-Id header");
        }

        return walletService.getWalletByAuthUserId(authUserId);
    }
}
