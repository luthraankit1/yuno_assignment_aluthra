package com.yuno.assignment.idempotency;

public interface IdempotencyStore {

    IdempotencyResult store(String key);

    void commit(String key, String responseJson);

    void release(String key);
}
