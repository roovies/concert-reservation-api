package com.roovies.concertreservation.reservations.application.dto.result;

import java.util.List;

public record HoldSeatResult(
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        long ttlSeconds
) {
    public static HoldSeatResult of(Long scheduleId, List<Long> seatIds, Long userId, long ttlSeconds) {
        return new HoldSeatResult(scheduleId, seatIds, userId, ttlSeconds);
    }
}