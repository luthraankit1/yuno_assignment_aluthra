package com.yuno.assignment.support;

import com.yuno.assignment.api.dto.PaymentRequest;
import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.persistence.entity.enums.Status;

import java.math.BigDecimal;

public final class PaymentTestFixtures {

    public static final String VALID_CUSTOMER_ID = "customer_1234567890";
    public static final String VALID_TOKEN = "pm_tok_visa_test_001";

    public static final String COUNTRY_DEU = "DEU";
    public static final String COUNTRY_IND = "IND";

    private PaymentTestFixtures() {
    }

    public static PaymentRequest validEurRequest() {
        return new PaymentRequest(
                VALID_CUSTOMER_ID,
                new BigDecimal("100.00"),
                "EUR",
                COUNTRY_DEU,
                VALID_TOKEN);
    }

    public static PaymentRequest validInrRequest() {
        return new PaymentRequest(
                VALID_CUSTOMER_ID,
                new BigDecimal("500.00"),
                "INR",
                COUNTRY_IND,
                VALID_TOKEN);
    }

    public static PaymentRequest request(String currency, String country, BigDecimal amount) {
        return new PaymentRequest(VALID_CUSTOMER_ID, amount, currency, country, VALID_TOKEN);
    }

    public static Payment payment(String currency, String country, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setCustomerId(VALID_CUSTOMER_ID);
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment.setCountry(country);
        payment.setPaymentMethodToken(VALID_TOKEN);
        payment.setStatus(Status.INITIATED);
        return payment;
    }
}
