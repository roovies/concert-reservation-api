package com.roovies.concertreservation.payments.application.dto.command;

import java.util.List;

public record PayReservationCommand(
        String idempotencyKey,
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long payForAmount,
        Long discountAmount
) {
    public static PayReservationCommand of(
            String idempotencyKey,
            Long scheduleId,
            List<Long> seatIds,
            Long userId,
            Long payForAmount,
            Long discountAmount
    ) {
        return new PayReservationCommand(
                idempotencyKey,
                scheduleId,
                seatIds,
                userId,
                payForAmount,
                discountAmount
        );
    }
}
