package com.roovies.concertreservation.payments.application.dto.query;

import java.util.List;

public record GetHeldSeatListQuery(
        Long scheduleId,
        List<Long> seatIds,
        Long userId
) {
    public static GetHeldSeatListQuery of(Long scheduleId, List<Long> seatIds, Long userId) {
        return new GetHeldSeatListQuery(scheduleId, seatIds, userId);
    }
}
