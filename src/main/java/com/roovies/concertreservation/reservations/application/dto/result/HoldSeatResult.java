package com.roovies.concertreservation.reservations.application.dto.result;

import lombok.Builder;

import java.util.List;

@Builder
public record HoldSeatResult(
        Long scheduleId,
        List<Long> seatIds,
        Long userId,
        Long totalPrice,
        long ttlSeconds
) {}