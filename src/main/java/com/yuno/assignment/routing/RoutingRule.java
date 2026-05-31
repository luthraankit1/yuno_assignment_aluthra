package com.yuno.assignment.routing;

import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.provider.PaymentProvider;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class RoutingRule {

    private String currency;
    private String country;
    private BigDecimal amountMax;
    private List<PaymentProvider> providers = Collections.emptyList();

    public boolean matches(Payment payment) {
        if (currency != null && !currency.equalsIgnoreCase(payment.getCurrency())) {
            return false;
        }
        if (country != null && !country.equalsIgnoreCase(payment.getCountry())) {
            return false;
        }
        if (amountMax != null && payment.getAmount().compareTo(amountMax) > 0) {
            return false;
        }
        return true;
    }
}
