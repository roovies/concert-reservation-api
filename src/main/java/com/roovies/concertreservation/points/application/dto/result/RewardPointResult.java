package com.roovies.concertreservation.points.application.dto.result;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 포인트 적립 결과.
 *
 * @param userId 사용자 ID
 * @param rewardAmount 적립된 포인트 금액
 * @param totalAmount 적립 후 총 포인트 잔액
 * @param updatedAt 포인트 갱신 시각
 */
@Builder
public record RewardPointResult(
        Long userId,
        Long rewardAmount,
        Long totalAmount,
        LocalDateTime updatedAt
) {
}