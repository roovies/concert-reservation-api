package com.roovies.concertreservation.shared.domain.event;

import java.time.LocalDateTime;

/**
 * 결제 보상 Kafka 이벤트.
 * <p>
 * SAGA 워크플로우에서 하위 단계(포인트 적립 등)가 실패했을 때
 * 이미 완료된 결제 트랜잭션을 보상(포인트 환불)하기 위해 발행되는 이벤트.
 *
 * @param paymentId 결제 ID
 * @param userId 사용자 ID
 * @param refundAmount 환불할 금액
 * @param reason 보상 사유
 * @param publishedAt 이벤트 발행 시각
 */
public record CompensatePaymentEvent(
        Long paymentId,
        Long userId,
        Long refundAmount,
        String reason,
        LocalDateTime publishedAt
) {
    public static CompensatePaymentEvent of(
            Long paymentId,
            Long userId,
            Long refundAmount,
            String reason
    ) {
        return new CompensatePaymentEvent(
                paymentId,
                userId,
                refundAmount,
                reason,
                LocalDateTime.now()
        );
    }
}