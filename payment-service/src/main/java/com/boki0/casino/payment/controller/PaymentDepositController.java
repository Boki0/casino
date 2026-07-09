package com.boki0.casino.payment.controller;

import com.boki0.casino.payment.dto.CreateDepositRequest;
import com.boki0.casino.payment.dto.DepositResponse;
import com.boki0.casino.payment.service.DepositService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/deposits")
public class PaymentDepositController {

    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";

    private final DepositService depositService;

    public PaymentDepositController(DepositService depositService) {
        this.depositService = depositService;
    }

    @PostMapping
    public ResponseEntity<DepositResponse> createDeposit(
            @RequestHeader(value = HEADER_AUTH_USER_ID, required = false) String authUserIdHeader,
            @Valid @RequestBody CreateDepositRequest request
    ) {
        UUID authUserId = parseAuthUserId(authUserIdHeader);
        DepositResponse response = depositService.createDeposit(authUserId, request);

        return ResponseEntity
                .created(URI.create("/payments/deposits/" + response.depositId()))
                .body(response);
    }

    private UUID parseAuthUserId(String authUserIdHeader) {
        if (authUserIdHeader == null || authUserIdHeader.isBlank()) {
            throw new IllegalArgumentException("Missing X-Auth-User-Id header");
        }

        try {
            return UUID.fromString(authUserIdHeader);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Invalid X-Auth-User-Id header");
        }
    }
}
