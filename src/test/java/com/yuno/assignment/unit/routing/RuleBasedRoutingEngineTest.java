package com.yuno.assignment.unit.routing;

import com.yuno.assignment.exception.PaymentProviderNotFoundException;
import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.persistence.repository.PaymentRepository;
import com.yuno.assignment.properties.RoutingProperties;
import com.yuno.assignment.provider.PaymentProvider;
import com.yuno.assignment.provider.ProviderRegistry;
import com.yuno.assignment.provider.connector.ProviderConnector;
import com.yuno.assignment.provider.impl.ProviderA;
import com.yuno.assignment.provider.impl.ProviderB;
import com.yuno.assignment.routing.RoutingRule;
import com.yuno.assignment.routing.impl.RuleBasedRoutingEngine;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class RuleBasedRoutingEngineTest {

    @Mock
    private RoutingProperties routingProperties;

    @Mock
    private ProviderRegistry providerRegistry;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ProviderConnector connectorA;

    @Mock
    private ProviderConnector connectorB;

    private PaymentProvider providerA;
    private PaymentProvider providerB;
    private RuleBasedRoutingEngine engine;

    @BeforeEach
    void setUp() {
        providerA = new ProviderA();
        providerB = new ProviderB();
        engine = new RuleBasedRoutingEngine(routingProperties, providerRegistry, paymentRepository);
        when(connectorA.provider()).thenReturn(providerA);
        when(connectorB.provider()).thenReturn(providerB);
    }

    @Test
    @DisplayName("positive: first matching rule determines provider route")
    void selectsFirstMatchingRule() {
        RoutingRule eurRule = rule("EUR", providerA);
        RoutingRule inrRule = rule("INR", providerB);
        when(routingProperties.getRules()).thenReturn(Arrays.asList(eurRule, inrRule));
        when(providerRegistry.require(providerA)).thenReturn(connectorA);

        Payment payment = PaymentTestFixtures.payment("EUR", PaymentTestFixtures.COUNTRY_DEU, new BigDecimal("50.00"));
        List<ProviderConnector> route = engine.selectRoute(payment);

        assertThat(route).containsExactly(connectorA);
    }

    @Test
    @DisplayName("positive: falls back to default providers when no rule matches")
    void usesDefaultProvidersWhenNoRuleMatches() {
        when(routingProperties.getRules()).thenReturn(Collections.emptyList());
        when(routingProperties.getDefaultProviders()).thenReturn(Arrays.asList(providerA, providerB));
        when(providerRegistry.require(providerA)).thenReturn(connectorA);
        when(providerRegistry.require(providerB)).thenReturn(connectorB);

        Payment payment = PaymentTestFixtures.payment("USD", "USA", new BigDecimal("50.00"));
        List<ProviderConnector> route = engine.selectRoute(payment);

        assertThat(route).containsExactly(connectorA, connectorB);
    }

    @Test
    @DisplayName("negative: throws when no rule and no default providers")
    void throwsWhenNoProvidersConfigured() {
        when(routingProperties.getRules()).thenReturn(Collections.emptyList());
        when(routingProperties.getDefaultProviders()).thenReturn(Collections.emptyList());

        Payment payment = PaymentTestFixtures.payment("USD", "USA", new BigDecimal("50.00"));

        assertThatThrownBy(() -> engine.selectRoute(payment))
                .isInstanceOf(PaymentProviderNotFoundException.class);
        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    @DisplayName("edge: empty provider list on matched rule is treated as misconfiguration")
    void throwsWhenMatchedRuleHasEmptyProviders() {
        RoutingRule emptyRule = new RoutingRule();
        emptyRule.setCurrency("EUR");
        emptyRule.setProviders(Collections.emptyList());
        when(routingProperties.getRules()).thenReturn(Collections.singletonList(emptyRule));
        when(routingProperties.getDefaultProviders()).thenReturn(Collections.emptyList());

        Payment payment = PaymentTestFixtures.payment("EUR", PaymentTestFixtures.COUNTRY_DEU, new BigDecimal("10.00"));

        assertThatThrownBy(() -> engine.selectRoute(payment))
                .isInstanceOf(PaymentProviderNotFoundException.class);
    }

    private static RoutingRule rule(String currency, PaymentProvider provider) {
        RoutingRule rule = new RoutingRule();
        rule.setCurrency(currency);
        rule.setProviders(Collections.singletonList(provider));
        return rule;
    }
}
