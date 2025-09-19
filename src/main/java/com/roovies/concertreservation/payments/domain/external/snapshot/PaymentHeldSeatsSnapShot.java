package com.roovies.concertreservation.payments.domain.external.snapshot;

import java.util.List;

public record PaymentHeldSeatsSnapShot(
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long totalPrice
) {
    public static PaymentHeldSeatsSnapShot of(Long scheduleId, List<Long> seatIds, Long userId, Long totalPrice) {
        return new PaymentHeldSeatsSnapShot(scheduleId, seatIds, userId, totalPrice);
    }
}
