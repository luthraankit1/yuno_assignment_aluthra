package com.yuno.assignment.persistence.entity;

import com.yuno.assignment.persistence.entity.enums.Status;
import com.yuno.assignment.provider.PaymentProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payment")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @Column(name = "id", updatable = false, nullable = false)
    private int id;

    @Column(name = "customer_id", nullable = false, length = 64)
    private String customerId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "payment_method_token", nullable = false, length = 128)
    private String paymentMethodToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "selected_provider", length = 64)
    private String selectedProvider;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    @Setter(AccessLevel.NONE)
    private Long version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = Status.INITIATED;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(PaymentProvider paymentProvider, String providerRef) {
        this.status = Status.SUCCEEDED;
        this.selectedProvider = paymentProvider.getId();
        this.providerReference = providerRef;
        this.failureReason = null;
    }

    public void markFailed(PaymentProvider paymentProvider, String reason) {
        this.status = Status.FAILED;
        if (paymentProvider != null)
            this.selectedProvider = paymentProvider.getId();
        this.failureReason = reason;
    }
}
