package com.roovies.concertreservation.payments.domain.entity;

import com.roovies.concertreservation.payments.domain.enums.PaymentIdempotencyStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentIdempotency {
    private final String key;
    private final Long userId;
    private Long paymentId; // 결제 완료 시 설정
    private PaymentIdempotencyStatus status;
    private String resultData; // JSON으로 직렬화된 결과
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static PaymentIdempotency tryProcess(String key, Long userId) {
        return new PaymentIdempotency(
                key,
                userId,
                null,
                PaymentIdempotencyStatus.PROCESSING,
                null,
                LocalDateTime.now(),
                null
        );
    }

    public static PaymentIdempotency create(String key, Long userId, Long paymentId,
                                            PaymentIdempotencyStatus status, String resultData,
                                            LocalDateTime createdAt, LocalDateTime completedAt) {
        return new PaymentIdempotency(
                key,
                userId,
                paymentId,
                status,
                resultData,
                createdAt,
                completedAt
        );
    }

    private PaymentIdempotency(String key, Long userId, Long paymentId,
                               PaymentIdempotencyStatus status, String resultData,
                               LocalDateTime createdAt, LocalDateTime completedAt) {
        this.key = key;
        this.userId = userId;
        this.paymentId = paymentId;
        this.status = status;
        this.resultData = resultData;
        this.createdAt = createdAt;
        this.completedAt = completedAt;
    }

    public boolean isProcessing() {
        return this.status == PaymentIdempotencyStatus.PROCESSING;
    }

    public boolean isSuccess() {
        return this.status == PaymentIdempotencyStatus.SUCCESS;
    }

    public void setResult(Long paymentId, String resultData) {
        this.paymentId = paymentId;
        this.resultData = resultData;
        this.status = PaymentIdempotencyStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }
}
