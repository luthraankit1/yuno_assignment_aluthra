package com.yuno.assignment.sanity;

import com.yuno.assignment.api.controller.PaymentController;
import com.yuno.assignment.api.dto.PaymentResponse;
import com.yuno.assignment.persistence.entity.enums.Status;
import com.yuno.assignment.service.PaymentService;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("sanity")
class PaymentControllerSanityTest {

    @Mock
    private PaymentService paymentService;

    private PaymentController controller;

    @BeforeEach
    void setUp() {
        controller = new PaymentController(paymentService);
    }

    @Test
    @DisplayName("sanity: successful payment returns 201 Created")
    void successfulPaymentReturnsCreated() {
        PaymentResponse response = new PaymentResponse(
                1,
                PaymentTestFixtures.VALID_CUSTOMER_ID,
                new BigDecimal("100.00"),
                "EUR",
                PaymentTestFixtures.COUNTRY_DEU,
                Status.SUCCEEDED,
                "providerA",
                "A-ref",
                null,
                null,
                null);
        when(paymentService.processPayment(eq("idem-ok"), any())).thenReturn(response);

        ResponseEntity<PaymentResponse> result =
                controller.create("idem-ok", PaymentTestFixtures.validEurRequest());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(Status.SUCCEEDED);
        verify(paymentService).processPayment(eq("idem-ok"), any());
    }

    @Test
    @DisplayName("sanity: failed payment outcome returns 200 OK")
    void failedPaymentReturnsOk() {
        PaymentResponse response = new PaymentResponse(
                2,
                PaymentTestFixtures.VALID_CUSTOMER_ID,
                new BigDecimal("100.00"),
                "EUR",
                PaymentTestFixtures.COUNTRY_DEU,
                Status.FAILED,
                "providerA",
                null,
                "DECLINED: denied",
                null,
                null);
        when(paymentService.processPayment(eq("idem-fail"), any())).thenReturn(response);

        ResponseEntity<PaymentResponse> result =
                controller.create("idem-fail", PaymentTestFixtures.validEurRequest());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getStatus()).isEqualTo(Status.FAILED);
    }

    @Test
    @DisplayName("sanity: controller maps succeeded vs failed HTTP status codes")
    void httpStatusMappingMatchesPaymentOutcome() {
        when(paymentService.processPayment(eq("idem-ok"), any()))
                .thenReturn(new PaymentResponse(
                        1, PaymentTestFixtures.VALID_CUSTOMER_ID, new BigDecimal("1.00"),
                        "EUR", PaymentTestFixtures.COUNTRY_DEU, Status.SUCCEEDED,
                        "providerA", null, null, null, null));
        when(paymentService.processPayment(eq("idem-fail"), any()))
                .thenReturn(new PaymentResponse(
                        2, PaymentTestFixtures.VALID_CUSTOMER_ID, new BigDecimal("1.00"),
                        "EUR", PaymentTestFixtures.COUNTRY_DEU, Status.FAILED,
                        "providerA", null, "err", null, null));

        assertThat(controller.create("idem-ok", PaymentTestFixtures.validEurRequest()).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(controller.create("idem-fail", PaymentTestFixtures.validEurRequest()).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }
}
