package com.yuno.assignment.sanity;

import com.yuno.assignment.idempotency.IdempotencyStore;
import com.yuno.assignment.routing.RoutingEngine;
import com.yuno.assignment.service.PaymentService;
import com.yuno.assignment.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("sanity")
class ApplicationSanityTest extends AbstractIntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private RoutingEngine routingEngine;

    @Autowired
    private IdempotencyStore idempotencyStore;

    @Test
    @DisplayName("sanity: Spring context loads core beans")
    void contextLoadsCoreBeans() {
        assertThat(paymentService).isNotNull();
        assertThat(routingEngine).isNotNull();
        assertThat(idempotencyStore).isNotNull();
    }
}
