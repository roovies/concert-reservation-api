package com.roovies.concertreservation.shared.domain.event;

import java.time.LocalDateTime;

/**
 * 포인트 적립 완료 Kafka 이벤트.
 * <p>
 * 결제 완료 후 포인트 적립이 성공적으로 완료되었을 때 발행되는 이벤트로,
 * SAGA Orchestration 패턴에서 다음 단계(알림 등)를 트리거하는 데 사용된다.
 *
 * @param paymentId 결제 ID
 * @param userId 사용자 ID
 * @param rewardAmount 적립된 포인트 금액
 * @param totalPointAmount 적립 후 총 포인트 잔액
 * @param completedAt 포인트 적립 완료 시각
 * @param publishedAt 이벤트 발행 시각
 */
public record PointRewardCompletedEvent(
        Long paymentId,
        Long userId,
        Long rewardAmount,
        Long totalPointAmount,
        LocalDateTime completedAt,
        LocalDateTime publishedAt
) {
    public static PointRewardCompletedEvent of(
            Long paymentId,
            Long userId,
            Long rewardAmount,
            Long totalPointAmount,
            LocalDateTime completedAt
    ) {
        return new PointRewardCompletedEvent(
                paymentId,
                userId,
                rewardAmount,
                totalPointAmount,
                completedAt,
                LocalDateTime.now()
        );
    }
}