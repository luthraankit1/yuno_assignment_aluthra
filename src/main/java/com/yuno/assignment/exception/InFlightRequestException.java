package com.yuno.assignment.exception;

/**
 * Thrown when a request with the same idempotency key is already being processed. (HTTP 409)
 */
public class InFlightRequestException extends RuntimeException {

    public InFlightRequestException(String idempotencyKey) {
        super("A request with idempotency key '" + idempotencyKey + "' is already in flight");
    }
}
