package com.roovies.concertreservation.reservations.application.dto.command;

import java.util.List;

public record HoldSeatCommand(
        String idempotencyKey, // 멱등성 키 UUID
        Long scheduleId,
        List<Long> seatIds,
        Long userId
) {
    public static HoldSeatCommand of(String idempotencyKey, Long scheduleId, List<Long> seatIds, Long userId) {
        return new HoldSeatCommand(idempotencyKey, scheduleId, seatIds, userId);
    }
}
