package com.roovies.concertreservation.shared.domain.event;

import java.time.LocalDateTime;

/**
 * 포인트 적립 실패 Kafka 이벤트.
 * <p>
 * 결제 완료 후 포인트 적립이 실패했을 때 발행되는 이벤트로,
 * SAGA Orchestration 패턴에서 보상 트랜잭션(결제 취소)을 트리거하는 데 사용된다.
 *
 * @param paymentId 결제 ID
 * @param userId 사용자 ID
 * @param rewardAmount 적립 시도한 포인트 금액
 * @param reason 실패 사유
 * @param failedAt 포인트 적립 실패 시각
 * @param publishedAt 이벤트 발행 시각
 */
public record PointRewardFailedEvent(
        Long paymentId,
        Long userId,
        Long rewardAmount,
        String reason,
        LocalDateTime failedAt,
        LocalDateTime publishedAt
) {
    public static PointRewardFailedEvent of(
            Long paymentId,
            Long userId,
            Long rewardAmount,
            String reason
    ) {
        return new PointRewardFailedEvent(
                paymentId,
                userId,
                rewardAmount,
                reason,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}