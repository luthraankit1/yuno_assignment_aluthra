package com.yuno.assignment.unit.service;

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
import com.yuno.assignment.provider.impl.ProviderA;
import com.yuno.assignment.provider.impl.ProviderB;
import com.yuno.assignment.routing.RoutingEngine;
import com.yuno.assignment.service.impl.PaymentServiceImpl;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.COMPLETED;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.IN_FLIGHT;
import static com.yuno.assignment.idempotency.IdempotencyResult.Outcome.RESERVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("unit")
class PaymentServiceImplTest {

    @Mock
    private IdempotencyStore idempotencyStore;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentAttemptRepository attemptRepository;
    @Mock
    private RoutingEngine routingEngine;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private ProviderConnector primaryConnector;
    @Mock
    private ProviderConnector fallbackConnector;

    private PaymentServiceImpl paymentService;
    private ObjectMapper objectMapper;
    private PaymentProvider providerA;
    private PaymentProvider providerB;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        providerA = new ProviderA();
        providerB = new ProviderB();
        TransactionStatus txStatus = new SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(txStatus);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        paymentService = new PaymentServiceImpl(
                idempotencyStore,
                paymentRepository,
                attemptRepository,
                routingEngine,
                objectMapper,
                transactionTemplate);
    }

    @Test
    @DisplayName("positive: processes payment and commits idempotency cache on success")
    void processesPaymentSuccessfully() throws Exception {
        PaymentRequest request = PaymentTestFixtures.validEurRequest();
        Payment livePayment = savedPayment(1, request, Status.INITIATED);

        when(idempotencyStore.store("idem-1")).thenReturn(IdempotencyResult.getResult(RESERVED));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(1);
            return p;
        });
        when(paymentRepository.findById(1)).thenReturn(Optional.of(livePayment));
        when(routingEngine.selectRoute(any(Payment.class))).thenReturn(Collections.singletonList(primaryConnector));
        when(primaryConnector.provider()).thenReturn(providerA);
        when(primaryConnector.charge(any(Payment.class))).thenReturn(ProviderResponse.success("A-ref-1"));
        when(attemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> {
            PaymentAttempt a = inv.getArgument(0);
            a.setId(1);
            return a;
        });
        when(attemptRepository.findById(any(Integer.class))).thenAnswer(inv -> {
            PaymentAttempt a = new PaymentAttempt();
            a.setId(inv.getArgument(0));
            return Optional.of(a);
        });

        PaymentResponse response = paymentService.processPayment("idem-1", request);

        assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(response.getSelectedProvider()).isEqualTo(providerA.getId());
        assertThat(response.getProviderReference()).isEqualTo("A-ref-1");
        ArgumentCaptor<String> cached = ArgumentCaptor.forClass(String.class);
        verify(idempotencyStore).commit(eq("idem-1"), cached.capture());
        PaymentResponse cachedResponse = objectMapper.readValue(cached.getValue(), PaymentResponse.class);
        assertThat(cachedResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    }

    @Test
    @DisplayName("positive: replays cached response without charging providers")
    void replaysCachedIdempotentResponse() throws Exception {
        PaymentResponse cached = new PaymentResponse(
                99,
                PaymentTestFixtures.VALID_CUSTOMER_ID,
                PaymentTestFixtures.validEurRequest().getAmount(),
                "EUR",
                PaymentTestFixtures.COUNTRY_DEU,
                Status.SUCCEEDED,
                "providerA",
                "A-ref",
                null,
                null,
                null);
        String json = objectMapper.writeValueAsString(cached);
        when(idempotencyStore.store("idem-2")).thenReturn(IdempotencyResult.getResult(COMPLETED, json));

        PaymentResponse response = paymentService.processPayment("idem-2", PaymentTestFixtures.validEurRequest());

        assertThat(response.getId()).isEqualTo(99);
        verify(routingEngine, never()).selectRoute(any());
        verify(idempotencyStore, never()).commit(any(), any());
    }

    @Test
    @DisplayName("negative: in-flight duplicate request raises conflict exception")
    void rejectsInFlightDuplicate() {
        when(idempotencyStore.store("idem-3")).thenReturn(IdempotencyResult.getResult(IN_FLIGHT));

        assertThatThrownBy(() -> paymentService.processPayment("idem-3", PaymentTestFixtures.validEurRequest()))
                .isInstanceOf(InFlightRequestException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("negative: releases idempotency lock when processing fails")
    void releasesIdempotencyOnFailure() {
        when(idempotencyStore.store("idem-4")).thenReturn(IdempotencyResult.getResult(RESERVED));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(2);
            return p;
        });
        when(routingEngine.selectRoute(any(Payment.class)))
                .thenThrow(new RuntimeException("routing exploded"));

        assertThatThrownBy(() -> paymentService.processPayment("idem-4", PaymentTestFixtures.validEurRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("routing exploded");
        verify(idempotencyStore).release("idem-4");
    }

    @Test
    @DisplayName("regression: failover tries next provider when first fails")
    void failoverToSecondProvider() {
        PaymentRequest request = PaymentTestFixtures.validEurRequest();
        Payment livePayment = savedPayment(3, request, Status.INITIATED);

        when(idempotencyStore.store("idem-5")).thenReturn(IdempotencyResult.getResult(RESERVED));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(3);
            return p;
        });
        when(paymentRepository.findById(3)).thenReturn(Optional.of(livePayment));
        when(routingEngine.selectRoute(any(Payment.class)))
                .thenReturn(Arrays.asList(primaryConnector, fallbackConnector));
        when(primaryConnector.provider()).thenReturn(providerA);
        when(fallbackConnector.provider()).thenReturn(providerB);
        when(primaryConnector.charge(any(Payment.class)))
                .thenReturn(ProviderResponse.failure("DECLINED", "Insufficient funds"));
        when(fallbackConnector.charge(any(Payment.class)))
                .thenReturn(ProviderResponse.success("B-ref"));
        when(attemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> {
            PaymentAttempt a = inv.getArgument(0);
            a.setId(a.getAttemptNumber());
            return a;
        });
        when(attemptRepository.findById(any(Integer.class))).thenAnswer(inv -> {
            PaymentAttempt a = new PaymentAttempt();
            a.setId(inv.getArgument(0));
            return Optional.of(a);
        });

        PaymentResponse response = paymentService.processPayment("idem-5", request);

        assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(response.getSelectedProvider()).isEqualTo(providerB.getId());
        verify(primaryConnector).charge(any(Payment.class));
        verify(fallbackConnector).charge(any(Payment.class));
    }

    @Test
    @DisplayName("edge: provider exception is converted to failed attempt and failover continues")
    void handlesProviderException() {
        PaymentRequest request = PaymentTestFixtures.validEurRequest();
        Payment livePayment = savedPayment(4, request, Status.INITIATED);

        when(idempotencyStore.store("idem-6")).thenReturn(IdempotencyResult.getResult(RESERVED));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(4);
            return p;
        });
        when(paymentRepository.findById(4)).thenReturn(Optional.of(livePayment));
        when(routingEngine.selectRoute(any(Payment.class)))
                .thenReturn(Collections.singletonList(primaryConnector));
        when(primaryConnector.provider()).thenReturn(providerA);
        when(primaryConnector.charge(any(Payment.class))).thenThrow(new RuntimeException("timeout"));
        when(attemptRepository.save(any(PaymentAttempt.class))).thenAnswer(inv -> {
            PaymentAttempt a = inv.getArgument(0);
            a.setId(1);
            return a;
        });
        when(attemptRepository.findById(any(Integer.class))).thenAnswer(inv -> {
            PaymentAttempt a = new PaymentAttempt();
            a.setId(inv.getArgument(0));
            return Optional.of(a);
        });

        PaymentResponse response = paymentService.processPayment("idem-6", request);

        assertThat(response.getStatus()).isEqualTo(Status.FAILED);
    }

    private static Payment savedPayment(int id, PaymentRequest request, Status status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setCustomerId(request.getCustomerId());
        payment.setAmount(request.getAmount());
        payment.setCurrency(request.getCurrency());
        payment.setCountry(request.getCountry());
        payment.setPaymentMethodToken(request.getPaymentMethodToken());
        payment.setStatus(status);
        return payment;
    }
}
