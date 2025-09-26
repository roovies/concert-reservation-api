package com.roovies.concertreservation.payments.domain.external;

import java.util.List;

public record ExternalHeldSeatList(
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long totalPrice
) {
    public static ExternalHeldSeatList of(Long scheduleId, List<Long> seatIds, Long userId, Long totalPrice) {
        return new ExternalHeldSeatList(scheduleId, seatIds, userId, totalPrice);
    }
}
