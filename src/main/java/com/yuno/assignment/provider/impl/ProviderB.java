package com.yuno.assignment.provider.impl;

import com.yuno.assignment.provider.PaymentProvider;

public class ProviderB extends PaymentProvider {

    public ProviderB() {
        super("providerB", "B", 40L);
    }

    public ProviderB(String id, String prefix, Long latencyMs) {
        super(id, prefix, latencyMs);
    }
}
