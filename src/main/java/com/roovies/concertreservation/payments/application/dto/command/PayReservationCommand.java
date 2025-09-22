package com.roovies.concertreservation.payments.application.dto.command;

import lombok.Builder;

import java.util.List;

@Builder
public record PayReservationCommand(
        String idempotencyKey,
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long payForAmount,
        Long discountAmount
) {}