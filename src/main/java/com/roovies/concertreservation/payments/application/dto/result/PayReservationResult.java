package com.roovies.concertreservation.payments.application.dto.result;

import com.roovies.concertreservation.payments.domain.enums.PaymentStatus;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
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
) {}
