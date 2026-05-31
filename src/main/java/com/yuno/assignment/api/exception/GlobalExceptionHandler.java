package com.yuno.assignment.api.exception;

import com.yuno.assignment.exception.InFlightRequestException;
import com.yuno.assignment.exception.PaymentProviderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream().map(this::formatFieldError).collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream().map(this::formatViolation).collect(Collectors.toList());
        return build(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed", details);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> handleMissingHeader(MissingRequestHeaderException ex) {
        return build(HttpStatus.BAD_REQUEST, "missing_header", "Required header is missing: " + ex.getHeaderName(), Collections.emptyList());
    }

    @ExceptionHandler(InFlightRequestException.class)
    public ResponseEntity<ApiError> handleInFlight(InFlightRequestException ex) {
        return build(HttpStatus.CONFLICT, "in_flight", ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, "bad_request", ex.getMessage(), Collections.emptyList());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred", Collections.emptyList());
    }

    @ExceptionHandler(PaymentProviderNotFoundException.class)
    public ResponseEntity<ApiError> handleNoProvider(Exception ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "unprocessable_entity", ex.getMessage(), Collections.emptyList());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String error, String message, List<String> details) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), error, message, details));
    }
}
