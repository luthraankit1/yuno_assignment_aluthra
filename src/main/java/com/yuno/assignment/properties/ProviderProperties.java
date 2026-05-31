package com.yuno.assignment.properties;

import com.yuno.assignment.provider.impl.ProviderA;
import com.yuno.assignment.provider.impl.ProviderB;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "providers")
public class ProviderProperties {

    private ProviderA providerA;
    private ProviderB providerB;

}
