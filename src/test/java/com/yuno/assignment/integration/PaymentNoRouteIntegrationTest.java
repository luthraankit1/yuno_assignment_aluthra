package com.yuno.assignment.integration;

import com.yuno.assignment.exception.PaymentProviderNotFoundException;
import com.yuno.assignment.service.PaymentService;
import com.yuno.assignment.support.AbstractIntegrationTest;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("integration")
@TestPropertySource(properties = {
        "routing.rules-json=[]"
})
class PaymentNoRouteIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("integration: no routing config throws PaymentProviderNotFoundException")
    void noProvidersThrowsNotFound() {
        assertThatThrownBy(() ->
                paymentService.processPayment("int-no-route", PaymentTestFixtures.validEurRequest()))
                .isInstanceOf(PaymentProviderNotFoundException.class);
    }
}
