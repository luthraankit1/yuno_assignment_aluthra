package com.yuno.assignment.provider.connector.impl;

import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.provider.PaymentProvider;
import com.yuno.assignment.provider.ProviderResponse;
import com.yuno.assignment.provider.connector.ProviderConnector;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class MockProviderConnector implements ProviderConnector {

    private final PaymentProvider paymentProvider;

    public MockProviderConnector(PaymentProvider provider) {
        this.paymentProvider = provider;
    }

    private static void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public PaymentProvider provider() {
        return paymentProvider;
    }

    @Override
    public ProviderResponse charge(Payment payment) {
        sleep(paymentProvider.getLatencyMs());
        String ref = paymentProvider.getPrefix() + "-" + UUID.randomUUID();
        log.info("Provider accepted payment {} as {}", payment.getId(), ref);
        return ProviderResponse.success(ref);
    }
}
