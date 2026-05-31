package com.yuno.assignment.provider.connector;

import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.provider.PaymentProvider;
import com.yuno.assignment.provider.ProviderResponse;

public interface ProviderConnector {

    PaymentProvider provider();
    ProviderResponse charge(Payment payment);
}
