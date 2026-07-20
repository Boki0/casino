package com.boki0.casino.payment.provider.stripe;

import com.boki0.casino.payment.config.StripeProperties;
import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.provider.CreateCheckoutCommand;
import com.boki0.casino.payment.provider.CreateCheckoutResult;
import com.boki0.casino.payment.provider.PaymentEventType;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.VerifiedPaymentEvent;
import com.stripe.Stripe;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StripePaymentProvider implements PaymentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripePaymentProvider.class);
    private static final String PLACEHOLDER_SECRET_KEY = "REPLACE_WITH_YOUR_LOCAL_TEST_SECRET_KEY";

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
        ensureStripeWebhookSecretConfigured();

        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException exception) {
            throw new IllegalArgumentException("Invalid Stripe webhook signature", exception);
        }
        LOGGER.info("Verified Stripe webhook stripeEventType={}", event.getType());

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = extractCheckoutSession(event);
            Map<String, String> metadata = new HashMap<>();
            if (session.getMetadata() != null) {
                metadata.putAll(session.getMetadata());
            }
            metadata.put("stripeEventType", event.getType());
            LOGGER.info(
                    "Stripe checkout.session.completed sessionId={} paymentIntentId={} hasDepositOrderId={} hasAuthUserId={} currency={} amountTotal={}",
                    session.getId(),
                    session.getPaymentIntent(),
                    hasMetadataValue(metadata, "depositOrderId"),
                    hasMetadataValue(metadata, "authUserId"),
                    session.getCurrency(),
                    session.getAmountTotal()
            );

            return new VerifiedPaymentEvent(
                    PaymentProviderType.STRIPE,
                    PaymentEventType.DEPOSIT_COMPLETED,
                    session.getId(),
                    session.getPaymentIntent(),
                    fromMinorUnits(session.getAmountTotal()),
                    toUppercaseCurrency(session.getCurrency()),
                    parseDepositOrderId(metadata),
                    metadata
            );
        }

        return new VerifiedPaymentEvent(
                PaymentProviderType.STRIPE,
                PaymentEventType.UNKNOWN,
                null,
                null,
                null,
                null,
                null,
                Map.of("stripeEventType", event.getType())
        );
    }

    private void ensureStripeSecretKeyConfigured() {
        String secretKey = stripeProperties.getSecretKey();

        if (secretKey == null || secretKey.isBlank() || PLACEHOLDER_SECRET_KEY.equals(secretKey)) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }
    }

    private void ensureStripeWebhookSecretConfigured() {
        String webhookSecret = stripeProperties.getWebhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Stripe webhook secret is not configured");
        }
    }

    private Session extractCheckoutSession(Event event) {
        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseGet(() -> deserializeUnsafe(event));

        if (!(stripeObject instanceof Session session)) {
            throw new IllegalArgumentException("Stripe checkout session payload is invalid");
        }

        return session;
    }

    private StripeObject deserializeUnsafe(Event event) {
        try {
            return event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (EventDataObjectDeserializationException exception) {
            throw new IllegalArgumentException("Stripe checkout session payload is invalid", exception);
        }
    }

    private BigDecimal fromMinorUnits(Long amountTotal) {
        if (amountTotal == null) {
            return null;
        }

        return BigDecimal.valueOf(amountTotal).movePointLeft(2);
    }

    private String toUppercaseCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return null;
        }

        return currency.toUpperCase(Locale.ROOT);
    }

    private UUID parseDepositOrderId(Map<String, String> metadata) {
        String depositOrderId = metadata.get("depositOrderId");
        if (depositOrderId == null || depositOrderId.isBlank()) {
            return null;
        }

        return UUID.fromString(depositOrderId);
    }

    private boolean hasMetadataValue(Map<String, String> metadata, String key) {
        String value = metadata.get(key);
        return value != null && !value.isBlank();
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
