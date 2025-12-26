package com.roovies.concertreservation.payments.application.dto.result;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 결제 환불 결과.
 *
 * @param paymentId 결제 ID
 * @param userId 사용자 ID
 * @param refundAmount 환불된 금액
 * @param totalPointAmount 환불 후 총 포인트 잔액
 * @param refundedAt 환불 처리 시각
 */
@Builder
public record RefundPaymentResult(
        Long paymentId,
        Long userId,
        Long refundAmount,
        Long totalPointAmount,
        LocalDateTime refundedAt
) {
}