package com.roovies.concertreservation.payments.application.dto.command;

import lombok.Builder;

/**
 * 결제 환불 요청 커맨드.
 * <p>
 * SAGA 보상 트랜잭션에서 사용되며, 포인트 적립 실패 등의 이유로
 * 결제를 롤백(포인트 환불)할 때 사용된다.
 *
 * @param paymentId 결제 ID
 * @param userId 사용자 ID
 * @param refundAmount 환불할 금액
 * @param reason 환불 사유
 */
@Builder
public record RefundPaymentCommand(
        Long paymentId,
        Long userId,
        Long refundAmount,
        String reason
) {
    public static RefundPaymentCommand of(
            Long paymentId,
            Long userId,
            Long refundAmount,
            String reason
    ) {
        return new RefundPaymentCommand(paymentId, userId, refundAmount, reason);
    }
}