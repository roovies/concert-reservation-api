package com.roovies.concertreservation.payments.domain.external.query;

import java.util.List;

public record HeldSeatsQuery(
        Long scheduleId,
        List<Long> seatIds,
        Long userId
) {
    public static HeldSeatsQuery of(Long scheduleId, List<Long> seatIds, Long userId) {
        return new HeldSeatsQuery(scheduleId, seatIds, userId);
    }
}
