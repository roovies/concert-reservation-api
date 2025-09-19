package com.roovies.concertreservation.payments.application.dto.result;

import com.roovies.concertreservation.payments.domain.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.List;

public record PayReservationResult(
        Long paymentId,
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long originalAmount,
        Long discountAmount,
        Long paidAmount,
        PaymentStatus status,
        LocalDateTime completedAt
) {
    public static PayReservationResult of(Long paymentId, Long scheduleId, List<Long> seatIds, Long userId, Long originalAmount, Long discountAmount, Long paidAmount, PaymentStatus status, LocalDateTime completedAt) {
        return new PayReservationResult(paymentId, scheduleId, seatIds, userId, originalAmount, discountAmount, paidAmount, status, completedAt);

    }
}
