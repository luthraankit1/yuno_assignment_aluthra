package com.yuno.assignment.unit.idempotency;

import com.yuno.assignment.idempotency.IdempotencyResult;
import com.yuno.assignment.idempotency.RedisIdempotencyStore;
import com.yuno.assignment.properties.IdempotencyProperties;
import com.yuno.assignment.support.InMemoryIdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.COMPLETED;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.IN_FLIGHT;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class RedisIdempotencyStoreTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisIdempotencyStore store;

    @BeforeEach
    void setUp() {
        IdempotencyProperties properties = new IdempotencyProperties();
        properties.setInFlightTtlSeconds(60);
        properties.setTtlSeconds(3600);
        when(redis.opsForValue()).thenReturn(valueOperations);
        store = new RedisIdempotencyStore(redis, properties);
    }

    @Test
    @DisplayName("positive: setIfAbsent reserves key as in-flight")
    void storeReservesWhenAbsent() {
        when(valueOperations.setIfAbsent(eq("pos:idem:key-1"), eq(InMemoryIdempotencyStore.IN_FLIGHT_MARKER), any(Duration.class)))
                .thenReturn(true);

        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(RESERVED);
    }

    @Test
    @DisplayName("negative: existing in-flight marker returns conflict outcome")
    void storeDetectsInFlight() {
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);
        when(valueOperations.get("pos:idem:key-1")).thenReturn(InMemoryIdempotencyStore.IN_FLIGHT_MARKER);

        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(IN_FLIGHT);
    }

    @Test
    @DisplayName("positive: completed payload is returned for duplicate requests")
    void storeReturnsCachedResponse() {
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);
        when(valueOperations.get("pos:idem:key-1")).thenReturn("{\"id\":1}");

        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(COMPLETED);
        assertThat(result.getCachedResponseJson()).isEqualTo("{\"id\":1}");
    }

    @Test
    @DisplayName("positive: commit persists serialized response with TTL")
    void commitStoresResponse() {
        store.commit("key-1", "{\"id\":1}");

        verify(valueOperations).set(eq("pos:idem:key-1"), eq("{\"id\":1}"), any(Duration.class));
    }

    @Test
    @DisplayName("edge: release deletes idempotency key")
    void releaseDeletesKey() {
        store.release("key-1");

        verify(redis).delete("pos:idem:key-1");
    }
}
