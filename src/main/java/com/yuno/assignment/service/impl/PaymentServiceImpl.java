package com.yuno.assignment.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yuno.assignment.api.dto.PaymentRequest;
import com.yuno.assignment.api.dto.PaymentResponse;
import com.yuno.assignment.exception.InFlightRequestException;
import com.yuno.assignment.idempotency.IdempotencyResult;
import com.yuno.assignment.idempotency.IdempotencyStore;
import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.persistence.entity.PaymentAttempt;
import com.yuno.assignment.persistence.entity.enums.Status;
import com.yuno.assignment.persistence.repository.PaymentAttemptRepository;
import com.yuno.assignment.persistence.repository.PaymentRepository;
import com.yuno.assignment.provider.PaymentProvider;
import com.yuno.assignment.provider.ProviderResponse;
import com.yuno.assignment.provider.connector.ProviderConnector;
import com.yuno.assignment.routing.RoutingEngine;
import com.yuno.assignment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.util.List;


@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    private final IdempotencyStore idempotencyStore;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final RoutingEngine routingEngine;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public PaymentServiceImpl(IdempotencyStore idempotencyStore,
                              PaymentRepository paymentRepository,
                              PaymentAttemptRepository attemptRepository,
                              RoutingEngine routingEngine,
                              ObjectMapper objectMapper,
                              TransactionTemplate transactionTemplate) {
        this.idempotencyStore = idempotencyStore;
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.routingEngine = routingEngine;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
        IdempotencyResult idempotencyResult = idempotencyStore.store(idempotencyKey);
        switch (idempotencyResult.getOutcome()) {
            case COMPLETED:
                log.info("Replaying cached response for idempotency key {}", idempotencyKey);
                return deserialize(idempotencyResult.getCachedResponseJson());
            case IN_FLIGHT:
                throw new InFlightRequestException(idempotencyKey);
            case FAILED:
                log.error("Failed to cache response for idempotency key {}", idempotencyKey);
                throw new RuntimeException(idempotencyResult.getCachedResponseJson());
            case RESERVED:
            default:
                // proceed
        }

        try {
            Payment payment = createInitialPayment(request);
            List<ProviderConnector> routes = routingEngine.selectRoute(payment);
            executeWithFailover(payment, routes);

            // Refresh from DB to capture updates
            Payment finalPayment = paymentRepository.findById(payment.getId()).orElse(payment);
            PaymentResponse response = PaymentResponse.fromEntity(finalPayment);
            idempotencyStore.commit(idempotencyKey, serialize(response));
            return response;
        } catch (RuntimeException ex) {
            // Release the in-flight marker so the client can retry instead of being blocked.
            idempotencyStore.release(idempotencyKey);
            throw ex;
        }
    }

    private Payment createInitialPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setCountry(request.getCountry());
        payment.setPaymentMethodToken(request.getPaymentMethodToken());
        payment.setStatus(Status.INITIATED);
        return transactionTemplate.execute(status -> paymentRepository.save(payment));
    }

    /**
     * Iterate through the route, recording each attempt and stopping on first success.
     * Each attempt is persisted in its own short transaction so the audit trail survives
     * even if a later attempt or post-processing step fails.
     */
    private void executeWithFailover(Payment payment, List<ProviderConnector> route) {
        updatePaymentStatus(payment, null, null, Status.IN_PROGRESS);

        int attemptNumber = 0;
        String lastErrorCode = null;
        String lastErrorMessage = null;
        PaymentProvider lastProvider = null;

        for (ProviderConnector connector : route) {
            attemptNumber++;
            lastProvider = connector.provider();
            PaymentAttempt attempt = startAttempt(payment, lastProvider, attemptNumber);

            ProviderResponse response;
            try {
                response = connector.charge(payment);
            } catch (RuntimeException ex) {
                log.warn("Provider {} threw for payment {}: {}", lastProvider.getId(), payment.getId(), ex.toString());
                response = ProviderResponse.failure("PROVIDER_EXCEPTION", ex.getMessage());
            }

            if (response.isSuccess()) {
                completeAttempt(attempt, Status.SUCCEEDED, response.getProviderReference(), null, null);
                updatePaymentStatus(payment, lastProvider, response.getProviderReference(), Status.SUCCEEDED);
                return;
            }

            completeAttempt(attempt, Status.FAILED, null, response.getErrorCode(), response.getErrorMessage());
            lastErrorCode = response.getErrorCode();
            lastErrorMessage = response.getErrorMessage();
            log.info("Provider {} failed for payment {} ({}: {}); trying next in route",
                    connector.provider().getId(), payment.getId(), lastErrorCode, lastErrorMessage);
        }

        updatePaymentStatus(payment, lastProvider,
                (lastErrorCode != null ? lastErrorCode + ": " : "") + lastErrorMessage,
                Status.FAILED);
    }

    private PaymentAttempt startAttempt(Payment payment, PaymentProvider provider, int attemptNumber) {
        return transactionTemplate.execute(status -> {
            PaymentAttempt attempt = new PaymentAttempt();
            attempt.setPayment(payment);
            attempt.setProviderId(provider.getId());
            attempt.setAttemptNumber(attemptNumber);
            attempt.setStatus(Status.IN_PROGRESS);
            return attemptRepository.save(attempt);
        });
    }

    private void completeAttempt(PaymentAttempt attempt,
                                 Status finalStatus,
                                 String providerRef,
                                 String errorCode,
                                 String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentAttempt managed = attemptRepository.findById(attempt.getId()).orElse(attempt);
            managed.complete(finalStatus, providerRef, errorCode, errorMessage);
            attemptRepository.save(managed);
        });
    }

    private void updatePaymentStatus(Payment payment, PaymentProvider paymentProvider, String info, Status status) {
        transactionTemplate.executeWithoutResult(transactionStatus -> {
            Payment managed = paymentRepository.findById(payment.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Payment vanished before status update: " + payment.getId()));

            switch (status) {
                case SUCCEEDED:
                    managed.markSucceeded(paymentProvider, info);
                    break;
                case FAILED:
                    managed.markFailed(paymentProvider, info);
                    break;
                case IN_PROGRESS:
                    managed.setStatus(status);
                    break;
                default:
                    break;
            }
            paymentRepository.save(managed);
        });
    }

    private String serialize(PaymentResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize PaymentResponse for idempotency cache", e);
        }
    }

    private PaymentResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, PaymentResponse.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize cached PaymentResponse", e);
        }
    }
}
