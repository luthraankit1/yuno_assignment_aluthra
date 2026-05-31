package com.yuno.assignment.provider.impl;

import com.yuno.assignment.provider.PaymentProvider;

public class ProviderA extends PaymentProvider {

    public ProviderA() {
        super("providerA", "A", 25L);
    }

    public ProviderA(String id, String prefix, Long latencyMs) {
        super(id, prefix, latencyMs);
    }
}
