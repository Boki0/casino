package com.boki0.casino.payment.provider;

import com.boki0.casino.payment.entity.PaymentProviderType;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class PaymentProviderRegistry {

    private final Map<PaymentProviderType, PaymentProvider> providers;

    public PaymentProviderRegistry(List<PaymentProvider> paymentProviders) {
        EnumMap<PaymentProviderType, PaymentProvider> providerMap = new EnumMap<>(PaymentProviderType.class);

        for (PaymentProvider paymentProvider : paymentProviders) {
            PaymentProviderType providerType = paymentProvider.providerType();
            PaymentProvider existingProvider = providerMap.putIfAbsent(providerType, paymentProvider);

            if (existingProvider != null) {
                throw new IllegalStateException(
                        "Duplicate payment provider registered for type: " + providerType
                );
            }
        }

        this.providers = Map.copyOf(providerMap);
    }

    public PaymentProvider getProvider(PaymentProviderType providerType) {
        PaymentProvider provider = providers.get(providerType);

        if (provider == null) {
            throw new IllegalArgumentException(
                    "No payment provider registered for type: " + providerType
            );
        }

        return provider;
    }
}
