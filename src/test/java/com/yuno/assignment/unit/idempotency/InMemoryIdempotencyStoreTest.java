package com.yuno.assignment.unit.idempotency;

import com.yuno.assignment.idempotency.IdempotencyResult;
import com.yuno.assignment.support.InMemoryIdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.COMPLETED;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.IN_FLIGHT;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class InMemoryIdempotencyStoreTest {

    private InMemoryIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotencyStore();
    }

    @Test
    @DisplayName("positive: first store reserves key")
    void firstStoreReservesKey() {
        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(RESERVED);
        assertThat(store.contains("key-1")).isTrue();
    }

    @Test
    @DisplayName("negative: concurrent store on same key reports in-flight")
    void secondStoreWhileInFlight() {
        store.store("key-1");

        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(IN_FLIGHT);
    }

    @Test
    @DisplayName("positive: commit allows replay of cached payload")
    void commitEnablesReplay() {
        store.store("key-1");
        store.commit("key-1", "{\"status\":\"SUCCEEDED\"}");

        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(COMPLETED);
        assertThat(result.getCachedResponseJson()).isEqualTo("{\"status\":\"SUCCEEDED\"}");
    }

    @Test
    @DisplayName("edge: release clears reservation so client can retry")
    void releaseAllowsRetry() {
        store.store("key-1");
        store.release("key-1");

        IdempotencyResult result = store.store("key-1");

        assertThat(result.getOutcome()).isEqualTo(RESERVED);
    }
}
