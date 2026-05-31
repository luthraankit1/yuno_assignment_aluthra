package com.yuno.assignment.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yuno.assignment.persistence.entity.Payment;
import com.yuno.assignment.persistence.entity.enums.Status;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
public class PaymentResponse {

    Integer id;
    String customerId;
    BigDecimal amount;
    String currency;
    String country;
    Status status;
    String selectedProvider;
    String providerReference;
    String failureReason;
    Instant createdAt;
    Instant updatedAt;

    @JsonCreator
    public PaymentResponse(@JsonProperty("id") int id,
                           @JsonProperty("customerId") String customerId,
                           @JsonProperty("amount") BigDecimal amount,
                           @JsonProperty("currency") String currency,
                           @JsonProperty("country") String country,
                           @JsonProperty("status") Status status,
                           @JsonProperty("selectedProvider") String selectedProvider,
                           @JsonProperty("providerReference") String providerReference,
                           @JsonProperty("failureReason") String failureReason,
                           @JsonProperty("createdAt") Instant createdAt,
                           @JsonProperty("updatedAt") Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.country = country;
        this.status = status;
        this.selectedProvider = selectedProvider;
        this.providerReference = providerReference;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentResponse fromEntity(Payment p) {
        return new PaymentResponse(
                p.getId(),
                p.getCustomerId(),
                p.getAmount(),
                p.getCurrency(),
                p.getCountry(),
                p.getStatus(),
                p.getSelectedProvider(),
                p.getProviderReference(),
                p.getFailureReason(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
