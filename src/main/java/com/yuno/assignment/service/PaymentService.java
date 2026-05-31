package com.yuno.assignment.service;

import com.yuno.assignment.api.dto.PaymentRequest;
import com.yuno.assignment.api.dto.PaymentResponse;

public interface PaymentService {

    PaymentResponse processPayment(String idempotencyKey, PaymentRequest request);
}
