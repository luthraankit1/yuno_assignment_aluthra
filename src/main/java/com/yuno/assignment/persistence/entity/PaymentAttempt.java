package com.yuno.assignment.persistence.entity;

import com.yuno.assignment.persistence.entity.enums.Status;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_attempt")
@Getter
@Setter
@NoArgsConstructor
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "global_seq")
    @Column(name = "id", updatable = false, nullable = false)
    private int id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "provider_id", nullable = false, length = 64)
    private String providerId;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private Status status;

    @Column(name = "provider_reference", length = 128)
    private String providerReference;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 512)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @PrePersist
    void onCreate() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
        if (this.status == null) {
            this.status = Status.IN_PROGRESS;
        }
    }

    public void complete(Status finalStatus, String providerRef, String errorCode, String errorMessage) {
        this.status = finalStatus;
        this.providerReference = providerRef;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.endedAt = Instant.now();
    }
}
