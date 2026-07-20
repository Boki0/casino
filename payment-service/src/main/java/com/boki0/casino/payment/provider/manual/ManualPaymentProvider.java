package com.boki0.casino.payment.provider.manual;

import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.provider.CreateCheckoutCommand;
import com.boki0.casino.payment.provider.CreateCheckoutResult;
import com.boki0.casino.payment.provider.PaymentEventType;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.VerifiedPaymentEvent;
import org.springframework.stereotype.Service;

@Service
public class ManualPaymentProvider implements PaymentProvider {

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.MANUAL;
    }

    @Override
    public CreateCheckoutResult createCheckoutSession(CreateCheckoutCommand command) {
        return new CreateCheckoutResult(
                PaymentProviderType.MANUAL,
                "manual-" + command.depositOrderId(),
                null
        );
    }

    @Override
    public VerifiedPaymentEvent verifyWebhook(String payload, String signatureHeader) {
        return new VerifiedPaymentEvent(
                PaymentProviderType.MANUAL,
                PaymentEventType.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
