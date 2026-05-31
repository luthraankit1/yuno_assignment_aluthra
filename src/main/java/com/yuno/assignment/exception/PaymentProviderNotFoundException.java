package com.yuno.assignment.exception;

import com.yuno.assignment.persistence.entity.Payment;

public class PaymentProviderNotFoundException extends RuntimeException {

    public PaymentProviderNotFoundException(Payment payment) {
        super("A provider with following parameters was not found " +
                "[Currency=" + payment.getCurrency() +
                ", Country=" + payment.getCountry() +
                ", Amount=" + payment.getAmount() +
                "]");
    }

}
