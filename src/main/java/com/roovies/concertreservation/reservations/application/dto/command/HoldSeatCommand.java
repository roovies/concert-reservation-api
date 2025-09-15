package com.roovies.concertreservation.reservations.application.dto.command;

import java.util.List;

public record HoldSeatCommand(
        Long scheduleId,
        List<Long> seatIds,
        Long userId
) {
    public static HoldSeatCommand of(Long scheduleId, List<Long> seatIds, Long userId) {
        return new HoldSeatCommand(scheduleId, seatIds, userId);
    }
}
