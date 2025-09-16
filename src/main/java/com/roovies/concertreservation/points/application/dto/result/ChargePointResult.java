package com.roovies.concertreservation.points.application.dto.result;

import java.time.LocalDateTime;

public record ChargePointResult(
        Long userId,
        long totalAmount,
        LocalDateTime updatedAt
) {
    public static ChargePointResult of(Long userId, long totalAmount, LocalDateTime updatedAt) {
        return new ChargePointResult(userId, totalAmount, updatedAt);
    }
}
