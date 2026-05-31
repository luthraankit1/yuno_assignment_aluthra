package com.yuno.assignment.unit.routing;

import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.routing.RoutingRule;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RoutingRuleTest {

    @Test
    @DisplayName("positive: matches when currency, country, and amount are within rule")
    void matchesWhenAllCriteriaMet() {
        RoutingRule rule = rule("EUR", "DEU", "500.00");
        Payment payment = PaymentTestFixtures.payment("EUR", "DEU", new BigDecimal("100.00"));

        assertThat(rule.matches(payment)).isTrue();
    }

    @Test
    @DisplayName("negative: rejects when currency differs")
    void rejectsWrongCurrency() {
        RoutingRule rule = rule("EUR", null, null);
        Payment payment = PaymentTestFixtures.payment("USD", "DEU", new BigDecimal("10.00"));

        assertThat(rule.matches(payment)).isFalse();
    }

    @Test
    @DisplayName("negative: rejects when country differs")
    void rejectsWrongCountry() {
        RoutingRule rule = rule(null, "DEU", null);
        Payment payment = PaymentTestFixtures.payment("EUR", "FRA", new BigDecimal("10.00"));

        assertThat(rule.matches(payment)).isFalse();
    }

    @Test
    @DisplayName("negative: rejects when amount exceeds maximum")
    void rejectsAmountAboveMax() {
        RoutingRule rule = rule("EUR", null, "500.00");
        Payment payment = PaymentTestFixtures.payment("EUR", "DEU", new BigDecimal("500.01"));

        assertThat(rule.matches(payment)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "500.00, true",
            "500.01, false"
    })
    @DisplayName("edge: amount exactly at max boundary")
    void amountAtBoundary(String amount, boolean expected) {
        RoutingRule rule = rule("EUR", null, "500.00");
        Payment payment = PaymentTestFixtures.payment("EUR", "DEU", new BigDecimal(amount));

        assertThat(rule.matches(payment)).isEqualTo(expected);
    }

    @Test
    @DisplayName("edge: null criteria on rule match any value for that field")
    void nullCriteriaMatchAny() {
        RoutingRule rule = new RoutingRule();
        Payment payment = PaymentTestFixtures.payment("XYZ", "ZZZ", new BigDecimal("99999.99"));

        assertThat(rule.matches(payment)).isTrue();
    }

    @Test
    @DisplayName("edge: currency comparison is case-insensitive")
    void currencyCaseInsensitive() {
        RoutingRule rule = rule("eur", null, null);
        Payment payment = PaymentTestFixtures.payment("EUR", "DEU", new BigDecimal("1.00"));

        assertThat(rule.matches(payment)).isTrue();
    }

    private static RoutingRule rule(String currency, String country, String amountMax) {
        RoutingRule rule = new RoutingRule();
        rule.setCurrency(currency);
        rule.setCountry(country);
        if (amountMax != null) {
            rule.setAmountMax(new BigDecimal(amountMax));
        }
        return rule;
    }
}
