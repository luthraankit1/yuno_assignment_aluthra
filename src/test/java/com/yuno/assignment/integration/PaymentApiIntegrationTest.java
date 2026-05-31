package com.yuno.assignment.integration;

import com.yuno.assignment.api.dto.PaymentResponse;
import com.yuno.assignment.persistence.entity.enums.Status;
import com.yuno.assignment.persistence.repository.PaymentAttemptRepository;
import com.yuno.assignment.persistence.repository.PaymentRepository;
import com.yuno.assignment.service.PaymentService;
import com.yuno.assignment.support.AbstractIntegrationTest;
import com.yuno.assignment.support.InMemoryIdempotencyStore;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;


@Tag("integration")
class PaymentApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private InMemoryIdempotencyStore idempotencyStore;

    @BeforeEach
    void cleanStores() {
        paymentAttemptRepository.deleteAll();
        paymentRepository.deleteAll();
        idempotencyStore.clear();
    }

    @Test
    @DisplayName("integration: EUR payment is routed to providerA and persisted")
    void eurPaymentSucceedsViaProviderA() {
        PaymentResponse response =
                paymentService.processPayment("int-eur-1", PaymentTestFixtures.validEurRequest());

        assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(response.getSelectedProvider()).isEqualTo("providerA");
        assertThat(response.getProviderReference()).isNotBlank();
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(paymentAttemptRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("integration: INR payment is routed to providerB")
    void inrPaymentUsesProviderB() {
        PaymentResponse response =
                paymentService.processPayment("int-inr-1", PaymentTestFixtures.validInrRequest());

        assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(response.getSelectedProvider()).isEqualTo("providerB");
    }

    @Test
    @DisplayName("integration: idempotent replay returns same payment without duplicate charge rows")
    void idempotentReplayReturnsSamePayment() {
        PaymentResponse first =
                paymentService.processPayment("int-idem-1", PaymentTestFixtures.validEurRequest());
        PaymentResponse second =
                paymentService.processPayment("int-idem-1", PaymentTestFixtures.validEurRequest());

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(paymentRepository.count()).isEqualTo(1);
        assertThat(paymentAttemptRepository.count()).isEqualTo(1);
    }
}
