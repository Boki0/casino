package com.boki0.casino.payment.provider;

import com.boki0.casino.payment.entity.PaymentProviderType;

public interface PaymentProvider {

    PaymentProviderType providerType();

    CreateCheckoutResult createCheckoutSession(CreateCheckoutCommand command);

    VerifiedPaymentEvent verifyWebhook(String payload, String signatureHeader);
}
