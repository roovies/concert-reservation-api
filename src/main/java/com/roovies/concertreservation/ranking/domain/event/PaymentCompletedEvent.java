package com.roovies.concertreservation.ranking.domain.event;

/**
 * 결제 완료 이벤트.
 * <p>
 * 결제가 성공적으로 완료되었을 때 발생하는 도메인 이벤트로,
 * 실시간 랭킹 계산을 위해 Redis Pub/Sub으로 전파된다.
 */
public record PaymentCompletedEvent(
        Long paymentId,
        Long scheduleId,
        Long userId
) {
    public static PaymentCompletedEvent of(Long paymentId, Long scheduleId, Long userId) {
        return new PaymentCompletedEvent(paymentId, scheduleId, userId);
    }
}