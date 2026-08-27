package com.boki0.casino.payment.controller;

import com.boki0.casino.payment.history.PaymentHistoryItemResponse;
import com.boki0.casino.payment.history.PaymentHistoryQuery;
import com.boki0.casino.payment.history.PaymentHistoryQueryService;
import com.boki0.casino.payment.history.PaymentHistoryType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/history")
public class PaymentHistoryController {

    private static final String HEADER_AUTH_USER_ID = "X-Auth-User-Id";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final PaymentHistoryQueryService paymentHistoryQueryService;

    public PaymentHistoryController(PaymentHistoryQueryService paymentHistoryQueryService) {
        this.paymentHistoryQueryService = paymentHistoryQueryService;
    }

    @GetMapping
    public Page<PaymentHistoryItemResponse> getPaymentHistory(
            @RequestHeader(value = HEADER_AUTH_USER_ID, required = false) String authUserIdHeader,
            @RequestParam(required = false) PaymentHistoryType type,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        UUID authenticatedUserId = parseAuthUserId(authUserIdHeader);
        validatePagination(page, size);

        LocalDateTime fromCreatedAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toCreatedAt = to == null ? null : to.atTime(LocalTime.MAX);
        PaymentHistoryQuery query = new PaymentHistoryQuery(type, fromCreatedAt, toCreatedAt);

        return paymentHistoryQueryService.findHistory(
                authenticatedUserId,
                query,
                PageRequest.of(page, size)
        );
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

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_PAGE_SIZE + "; default is " + DEFAULT_PAGE_SIZE
            );
        }
    }
}
