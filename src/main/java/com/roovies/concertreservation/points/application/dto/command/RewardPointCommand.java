package com.roovies.concertreservation.points.application.dto.command;

import lombok.Builder;

/**
 * 포인트 적립 요청 커맨드.
 *
 * @param userId 사용자 ID
 * @param rewardAmount 적립할 포인트 금액
 * @param paymentId 관련 결제 ID (추적용)
 */
@Builder
public record RewardPointCommand(
        Long userId,
        Long rewardAmount,
        Long paymentId
) {
    public static RewardPointCommand of(Long userId, Long rewardAmount, Long paymentId) {
        return new RewardPointCommand(userId, rewardAmount, paymentId);
    }
}