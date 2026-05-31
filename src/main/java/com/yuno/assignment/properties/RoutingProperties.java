package com.yuno.assignment.properties;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.yuno.assignment.provider.PaymentProvider;
import com.yuno.assignment.routing.RoutingRule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@ConfigurationProperties(prefix = "routing")
public class RoutingProperties {

    private String rulesJson;
    private List<RoutingRule> rules = Collections.emptyList();

    private ProviderProperties providerProperties;

    private List<PaymentProvider> defaultProviders = Collections.emptyList();

    public RoutingProperties(ProviderProperties providerProperties) {
        this.providerProperties = providerProperties;
    }

    @PostConstruct
    public void parse() throws JsonProcessingException {
        Map<String, PaymentProvider> providerMap = new HashMap<>();
        providerMap.put(providerProperties.getProviderA().getId(), providerProperties.getProviderA());
        providerMap.put(providerProperties.getProviderB().getId(), providerProperties.getProviderB());

        SimpleModule module = new SimpleModule();
        module.addDeserializer(PaymentProvider.class, new StdDeserializer<PaymentProvider>(PaymentProvider.class) {

            @Override
            public PaymentProvider deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
                String name = p.getText();
                PaymentProvider provider = providerMap.get(name);
                if (provider == null) throw new IllegalArgumentException("Unknown provider: " + name);
                return provider;
            }
        });

        if (rulesJson != null) {
            ObjectMapper mapper = new ObjectMapper().registerModule(module);
            this.rules = mapper.readValue(rulesJson, new TypeReference<List<RoutingRule>>() {
            });
        }
    }
}
