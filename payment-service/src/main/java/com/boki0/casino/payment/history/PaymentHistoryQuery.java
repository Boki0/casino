package com.boki0.casino.payment.history;

import java.time.LocalDateTime;

public record PaymentHistoryQuery(
        PaymentHistoryType type,
        LocalDateTime fromCreatedAt,
        LocalDateTime toCreatedAt
) {
    public PaymentHistoryQuery {
        if (fromCreatedAt != null && toCreatedAt != null && fromCreatedAt.isAfter(toCreatedAt)) {
            throw new IllegalArgumentException("fromCreatedAt must not be after toCreatedAt");
        }
    }

    public static PaymentHistoryQuery all() {
        return new PaymentHistoryQuery(null, null, null);
    }
}
