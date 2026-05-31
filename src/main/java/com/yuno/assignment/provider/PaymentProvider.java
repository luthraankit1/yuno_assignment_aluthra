package com.yuno.assignment.provider;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public abstract class PaymentProvider {
    protected String id;;
    protected String prefix;
    protected Long latencyMs;

    private PaymentProvider() {
        throw new RuntimeException("Method not Allowed");
    }
}
