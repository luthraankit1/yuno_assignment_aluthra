package com.yuno.assignment.api.exception;

import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
public class ApiError {

    Instant timestamp;
    int status;
    String error;
    String message;
    List<String> details;

    public static ApiError of(int status, String error, String message, List<String> details) {
        return new ApiError(Instant.now(), status, error, message, details);
    }
}
