package com.yuno.assignment.support;

import com.yuno.assignment.idempotency.IdempotencyStore;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestIdempotencyConfiguration {

    @Bean
    @Primary
    public InMemoryIdempotencyStore inMemoryIdempotencyStore() {
        return new InMemoryIdempotencyStore();
    }
}
