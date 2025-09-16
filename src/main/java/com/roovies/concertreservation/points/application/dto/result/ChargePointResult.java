package com.roovies.concertreservation.points.application.dto.result;

import java.time.Instant;

public record ChargePointResult(
        Long userId,
        long totalAmount,
        Instant updatedAt
) {
    public static ChargePointResult of(Long userId, long totalAmount, Instant updatedAt) {
        return new ChargePointResult(userId, totalAmount, updatedAt);
    }
}
