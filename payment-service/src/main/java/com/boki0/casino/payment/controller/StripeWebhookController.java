package com.boki0.casino.payment.controller;

import com.boki0.casino.payment.entity.PaymentProviderType;
import com.boki0.casino.payment.provider.PaymentEventType;
import com.boki0.casino.payment.provider.PaymentProvider;
import com.boki0.casino.payment.provider.PaymentProviderRegistry;
import com.boki0.casino.payment.provider.VerifiedPaymentEvent;
import com.boki0.casino.payment.service.DepositService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger LOGGER = LoggerFactory.getLogger(StripeWebhookController.class);
    private static final String HEADER_STRIPE_SIGNATURE = "Stripe-Signature";

    private final PaymentProviderRegistry paymentProviderRegistry;
    private final DepositService depositService;

    public StripeWebhookController(
            PaymentProviderRegistry paymentProviderRegistry,
            DepositService depositService
    ) {
        this.paymentProviderRegistry = paymentProviderRegistry;
        this.depositService = depositService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = HEADER_STRIPE_SIGNATURE, required = false) String signatureHeader
    ) {
        LOGGER.info(
                "Received Stripe webhook request signaturePresent={}",
                signatureHeader != null && !signatureHeader.isBlank()
        );

        try {
            PaymentProvider stripeProvider = paymentProviderRegistry.getProvider(PaymentProviderType.STRIPE);
            VerifiedPaymentEvent event = stripeProvider.verifyWebhook(payload, signatureHeader);
            LOGGER.info("Verified Stripe webhook eventType={}", event.eventType());

            if (event.eventType() == PaymentEventType.UNKNOWN) {
                LOGGER.info("Ignoring Stripe webhook event metadata={}", event.metadata());
                return ResponseEntity.ok(Map.of("status", "ignored"));
            }

            LOGGER.info(
                    "Handling Stripe checkout.session.completed providerSessionId={} depositOrderId={}",
                    event.providerSessionId(),
                    event.depositOrderId()
            );
            depositService.handleVerifiedPaymentEvent(event);
            LOGGER.info(
                    "Handled Stripe checkout.session.completed providerSessionId={} depositOrderId={}",
                    event.providerSessionId(),
                    event.depositOrderId()
            );

            return ResponseEntity.ok(Map.of("status", "processed"));
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Rejected Stripe webhook: {}", exception.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", exception.getMessage()
            ));
        } catch (Exception exception) {
            LOGGER.error("Failed to handle Stripe webhook: {}", exception.getMessage(), exception);
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "Stripe webhook handling failed"
            ));
        }
    }
}
