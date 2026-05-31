package com.yuno.assignment.routing.impl;

import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.persistence.repository.PaymentRepository;
import com.yuno.assignment.properties.RoutingProperties;
import com.yuno.assignment.provider.PaymentProvider;
import com.yuno.assignment.exception.PaymentProviderNotFoundException;
import com.yuno.assignment.provider.ProviderRegistry;
import com.yuno.assignment.provider.connector.ProviderConnector;
import com.yuno.assignment.routing.RoutingEngine;
import com.yuno.assignment.routing.RoutingRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RuleBasedRoutingEngine implements RoutingEngine {

    private final RoutingProperties routingProperties;
    private final ProviderRegistry providerRegistry;
    private final PaymentRepository paymentRepository;


    public RuleBasedRoutingEngine(RoutingProperties routingProperties, ProviderRegistry providerRegistry, PaymentRepository paymentRepository) {
        this.routingProperties = routingProperties;
        this.providerRegistry = providerRegistry;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public List<ProviderConnector> selectRoute(Payment payment) {
        List<PaymentProvider> providerIds = routingProperties.getRules().stream()
                .filter(rule -> rule.matches(payment))
                .findFirst()
                .map(RoutingRule::getProviders)
                .orElseGet(routingProperties::getDefaultProviders);

        if (providerIds == null || providerIds.isEmpty()) {
            payment.markFailed(null, "No routing config matched payment parameters");
            paymentRepository.save(payment);
            throw new PaymentProviderNotFoundException(payment);
        }

        List<ProviderConnector> providerConnectors = new ArrayList<>(providerIds.size());
        for (PaymentProvider paymentProvider : providerIds) {
            providerConnectors.add(providerRegistry.require(paymentProvider));
        }
        log.debug("Selected route {} for payment {}", providerIds, payment.getId());
        return providerConnectors;
    }
}
