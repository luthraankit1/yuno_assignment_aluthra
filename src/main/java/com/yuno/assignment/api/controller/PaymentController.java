package com.yuno.assignment.api.controller;

import com.yuno.assignment.api.dto.PaymentRequest;
import com.yuno.assignment.api.dto.PaymentResponse;
import com.yuno.assignment.persistence.entity.enums.Status;
import com.yuno.assignment.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final String PAYMENT_IDENTIFIER = "PAYMENT-IDENTIFIER";

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(value = PAYMENT_IDENTIFIER) String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(idempotencyKey, request);
        HttpStatus status = response.getStatus() == Status.SUCCEEDED
                ? HttpStatus.CREATED
                : HttpStatus.OK;
        return ResponseEntity.status(status).body(response);
    }
}
