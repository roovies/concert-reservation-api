package com.roovies.concertreservation.reservations.application.dto.command;

import lombok.Builder;

import java.util.List;

@Builder
public record HoldSeatCommand(
        String idempotencyKey, // 멱등성 키 UUID
        Long scheduleId,
        List<Long> seatIds,
        Long userId
) {}
