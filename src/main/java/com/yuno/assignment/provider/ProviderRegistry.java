package com.yuno.assignment.provider;

import com.yuno.assignment.provider.connector.ProviderConnector;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ProviderRegistry {

    private final Map<PaymentProvider, ProviderConnector> byId;

    public ProviderRegistry(List<ProviderConnector> connectors) {
        this.byId = Collections.unmodifiableMap(connectors.stream()
                .collect(Collectors.toMap(
                        ProviderConnector::provider,
                        Function.identity(),
                        (a, b) -> {
                            throw new IllegalStateException(
                                    "Duplicate provider id: " + a.provider().id);
                        },
                        LinkedHashMap::new)));
    }

    public Optional<ProviderConnector> find(PaymentProvider provider) {
        return Optional.ofNullable(byId.get(provider));
    }

    public ProviderConnector require(PaymentProvider provider) {
        return find(provider).orElseThrow(() ->
                new IllegalArgumentException("Unknown provider id: " + provider.getId()));
    }

    public Map<PaymentProvider, ProviderConnector> all() {
        return byId;
    }
}
