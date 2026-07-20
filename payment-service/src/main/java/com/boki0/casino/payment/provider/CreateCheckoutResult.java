package com.boki0.casino.payment.provider;

import com.boki0.casino.payment.entity.PaymentProviderType;

public record CreateCheckoutResult(
        PaymentProviderType provider,
        String providerSessionId,
        String checkoutUrl
) {
}
