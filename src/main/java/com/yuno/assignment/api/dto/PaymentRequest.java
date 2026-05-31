package com.yuno.assignment.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import javax.validation.constraints.*;
import java.math.BigDecimal;

@Value
public class PaymentRequest {

    @NotBlank
    @Size(min = 10, max = 64)
    String customerId;

    @NotNull
    @DecimalMin(value = "1.00", message = "amount must be at least 1.00")
    BigDecimal amount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "currency must be a 3-letter code")
    String currency;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$", message = "country must be a 3-letter code")
    String country;

    @NotBlank
    @Size(max = 128)
    String paymentMethodToken;

    @JsonCreator
    public PaymentRequest(@JsonProperty("customerId") String customerId,
                          @JsonProperty("amount") BigDecimal amount,
                          @JsonProperty("currency") String currency,
                          @JsonProperty("country") String country,
                          @JsonProperty("paymentMethodToken") String paymentMethodToken) {
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.country = country;
        this.paymentMethodToken = paymentMethodToken;
    }
}
