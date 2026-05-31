package com.yuno.assignment.support;

import com.yuno.assignment.idempotency.IdempotencyResult;
import com.yuno.assignment.idempotency.IdempotencyStore;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.*;

public class InMemoryIdempotencyStore implements IdempotencyStore {

    public static final String IN_FLIGHT_MARKER = "__IN_FLIGHT__";

    private final ConcurrentMap<String, String> entries = new ConcurrentHashMap<>();

    @Override
    public IdempotencyResult store(String key) {
        String previous = entries.putIfAbsent(key, IN_FLIGHT_MARKER);
        if (previous == null) {
            return IdempotencyResult.getResult(RESERVED);
        }
        if (IN_FLIGHT_MARKER.equals(previous)) {
            return IdempotencyResult.getResult(IN_FLIGHT);
        }
        return IdempotencyResult.getResult(COMPLETED, previous);
    }

    @Override
    public void commit(String key, String responseJson) {
        entries.put(key, responseJson);
    }

    @Override
    public void release(String key) {
        entries.remove(key);
    }

    public void clear() {
        entries.clear();
    }

    public boolean contains(String key) {
        return entries.containsKey(key);
    }
}
