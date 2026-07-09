package com.boki0.casino.payment.provider;

import com.boki0.casino.payment.entity.PaymentProviderType;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record VerifiedPaymentEvent(
        PaymentProviderType provider,
        PaymentEventType eventType,
        String providerSessionId,
        String providerPaymentId,
        BigDecimal amount,
        String currency,
        UUID depositOrderId,
        Map<String, String> metadata
) {
}
