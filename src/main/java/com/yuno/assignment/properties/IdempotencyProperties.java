package com.yuno.assignment.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    private long ttlSeconds = 86400L;

    private long inFlightTtlSeconds = 60L;

    public Duration getTtl() {
        return Duration.ofSeconds(ttlSeconds);
    }

    public Duration getInFlightTtl() {
        return Duration.ofSeconds(inFlightTtlSeconds);
    }
}
