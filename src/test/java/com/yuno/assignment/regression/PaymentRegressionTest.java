package com.yuno.assignment.regression;

import com.yuno.assignment.api.dto.PaymentResponse;
import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.persistence.entity.PaymentAttempt;
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

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("regression")
class PaymentRegressionTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentAttemptRepository paymentAttemptRepository;

    @Autowired
    private InMemoryIdempotencyStore idempotencyStore;

    @BeforeEach
    void resetData() {
        paymentAttemptRepository.deleteAll();
        paymentRepository.deleteAll();
        idempotencyStore.clear();
    }

    @Test
    @DisplayName("regression: payment amount at routing rule boundary still matches")
    void amountAtRoutingBoundaryStillMatches() {
        PaymentResponse response = paymentService.processPayment(
                "reg-boundary",
                PaymentTestFixtures.request("EUR", PaymentTestFixtures.COUNTRY_DEU, new BigDecimal("500.00")));

        assertThat(response.getSelectedProvider()).isEqualTo("providerA");
    }

    @Test
    @DisplayName("regression: amount above EUR cap matches secondary EUR rule (providerB)")
    void amountAboveEurCapUsesSecondaryEurRule() {
        PaymentResponse response = paymentService.processPayment(
                "reg-over-eur",
                PaymentTestFixtures.request("EUR", PaymentTestFixtures.COUNTRY_DEU, new BigDecimal("501.00")));

        assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(response.getSelectedProvider()).isEqualTo("providerB");

        List<PaymentAttempt> attempts = paymentAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getProviderId()).isEqualTo("providerB");
    }

    @Test
    @DisplayName("regression: each attempt is persisted for audit trail")
    void attemptsArePersistedForAudit() {
        paymentService.processPayment("reg-audit", PaymentTestFixtures.validEurRequest());

        Payment payment = paymentRepository.findAll().get(0);
        List<PaymentAttempt> attempts = paymentAttemptRepository.findAll();

        assertThat(payment.getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(attempts).hasSize(1);
        assertThat(attempts.get(0).getProviderId()).isEqualTo("providerA");
        assertThat(attempts.get(0).getStatus()).isEqualTo(Status.SUCCEEDED);
        assertThat(attempts.get(0).getProviderReference()).isNotNull();
    }

    @Test
    @DisplayName("regression: minimum valid amount is accepted")
    void minimumValidAmountAccepted() {
        PaymentResponse response = paymentService.processPayment(
                "reg-min-amount",
                PaymentTestFixtures.request("INR", PaymentTestFixtures.COUNTRY_IND, new BigDecimal("1.00")));

        assertThat(response.getStatus()).isEqualTo(Status.SUCCEEDED);
    }
}
