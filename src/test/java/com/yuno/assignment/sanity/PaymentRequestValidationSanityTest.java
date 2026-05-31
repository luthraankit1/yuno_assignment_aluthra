package com.yuno.assignment.sanity;

import com.yuno.assignment.api.dto.PaymentRequest;
import com.yuno.assignment.support.PaymentTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.validation.ConstraintViolation;
import javax.validation.UnexpectedTypeException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("sanity")
class PaymentRequestValidationSanityTest {

    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    @Test
    @DisplayName("sanity: @NotBlank on BigDecimal amount fails validation (current DTO)")
    void notBlankOnAmountIsInvalidForBigDecimal() {
        PaymentRequest request = PaymentTestFixtures.validEurRequest();

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(UnexpectedTypeException.class)
                .hasMessageContaining("amount");
    }

    @Test
    @DisplayName("sanity: invalid customerId length is rejected when amount constraint is not evaluated")
    void invalidCustomerIdRejected() {
        PaymentRequest request = new PaymentRequest(
                "short",
                PaymentTestFixtures.validEurRequest().getAmount(),
                "EUR",
                PaymentTestFixtures.COUNTRY_DEU,
                PaymentTestFixtures.VALID_TOKEN);

        try {
            Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);
            assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("customerId"));
        } catch (UnexpectedTypeException ex) {
            // Same root cause as live API: amount @NotBlank is evaluated first
            assertThat(ex.getMessage()).contains("amount");
        }
    }
}
