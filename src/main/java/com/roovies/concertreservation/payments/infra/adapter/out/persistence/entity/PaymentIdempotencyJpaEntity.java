package com.roovies.concertreservation.payments.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.payments.domain.entity.PaymentIdempotency;
import com.roovies.concertreservation.payments.domain.enums.PaymentIdempotencyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "payment_idempotency",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_created_at", columnList = "created_at") // 스케줄러 삭제용
        })
public class PaymentIdempotencyJpaEntity {
    @Id
    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name ="payment_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentIdempotencyStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason; // 실패 사유 필드 추가

    @Column(name = "result_data", columnDefinition = "TEXT")
    private String resultData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static PaymentIdempotencyJpaEntity from(PaymentIdempotency idempotency) {
        return new PaymentIdempotencyJpaEntity(
                idempotency.getKey(),
                idempotency.getUserId(),
                idempotency.getPaymentId(),
                idempotency.getStatus(),
                idempotency.getResultData(),
                idempotency.getCreatedAt(),
                idempotency.getCompletedAt()
        );
    }

    private PaymentIdempotencyJpaEntity(String idempotencyKey, Long userId, Long paymentId,
                                        PaymentIdempotencyStatus status, String resultData,
                                        LocalDateTime createdAt, LocalDateTime completedAt) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.paymentId = paymentId;
        this.status = status;
        this.resultData = resultData;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public void updateResult(Long paymentId, String resultData) {
        this.paymentId = paymentId;
        this.resultData = resultData;
        this.status = PaymentIdempotencyStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    public void updateFailed(String failureReason) {
        this.status = PaymentIdempotencyStatus.FAILED;
        this.failureReason = failureReason;
        this.completedAt = LocalDateTime.now();
    }
}
