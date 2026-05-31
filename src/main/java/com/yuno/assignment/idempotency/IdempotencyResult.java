package com.yuno.assignment.idempotency;

import lombok.Value;

@Value
public class IdempotencyResult {

    public enum Outcome {
        RESERVED,
        IN_FLIGHT,
        COMPLETED,
        FAILED
    }

    Outcome outcome;
    String cachedResponseJson;

    public static IdempotencyResult getResult(Outcome outcome, String response) {
        return new IdempotencyResult(outcome, response);
    }

    public static IdempotencyResult getResult(Outcome outcome) {
        return new IdempotencyResult(outcome, null);
    }
}
