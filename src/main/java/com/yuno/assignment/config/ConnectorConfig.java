package com.yuno.assignment.config;

import com.yuno.assignment.properties.ProviderProperties;
import com.yuno.assignment.provider.connector.ProviderConnector;
import com.yuno.assignment.provider.connector.impl.MockProviderConnector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConnectorConfig {

    @Bean
    public ProviderConnector providerAConnector(ProviderProperties providerProperties) {
        return new MockProviderConnector(providerProperties.getProviderA());
    }

    @Bean
    public ProviderConnector providerBConnector(ProviderProperties providerProperties) {
        return new MockProviderConnector(providerProperties.getProviderB());
    }
}