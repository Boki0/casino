package com.boki0.casino.payment.provider.stripe;

import com.boki0.casino.payment.config.StripeProperties;
import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.provider.CreateCheckoutCommand;
import com.boki0.casino.payment.provider.CreateCheckoutResult;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.VerifiedPaymentEvent;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentProvider implements PaymentProvider {

    private static final String PLACEHOLDER_SECRET_KEY = "sk_test_REPLACE_WITH_YOUR_LOCAL_TEST_SECRET_KEY";

    private final StripeProperties stripeProperties;

    public StripePaymentProvider(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public CreateCheckoutResult createCheckoutSession(CreateCheckoutCommand command) {
        ensureStripeSecretKeyConfigured();
        Stripe.apiKey = stripeProperties.getSecretKey();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(stripeProperties.getSuccessUrl())
                .setCancelUrl(stripeProperties.getCancelUrl())
                .setClientReferenceId(command.depositOrderId().toString())
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .putMetadata("depositOrderId", command.depositOrderId().toString())
                .putMetadata("authUserId", command.authUserId().toString())
                .putMetadata("creditsAmount", command.creditsAmount().toPlainString())
                .putMetadata("provider", PaymentProviderType.STRIPE.name())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(command.currency().toLowerCase(Locale.ROOT))
                                .setUnitAmount(toMinorUnits(command.amount()))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Casino demo credits")
                                        .build())
                                .build())
                        .build())
                .build();

        try {
            Session session = Session.create(params);

            return new CreateCheckoutResult(
                    PaymentProviderType.STRIPE,
                    session.getId(),
                    session.getUrl()
            );
        } catch (StripeException exception) {
            throw new IllegalStateException("Failed to create Stripe Checkout Session", exception);
        }
    }

    @Override
    public VerifiedPaymentEvent verifyWebhook(String payload, String signatureHeader) {
        throw new UnsupportedOperationException("Stripe webhook handling is not implemented yet");
    }

    private void ensureStripeSecretKeyConfigured() {
        String secretKey = stripeProperties.getSecretKey();

        if (secretKey == null || secretKey.isBlank() || PLACEHOLDER_SECRET_KEY.equals(secretKey)) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }
    }

    private Long toMinorUnits(BigDecimal amount) {
        try {
            return amount
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount cannot be converted to Stripe minor units", exception);
        }
    }
}
