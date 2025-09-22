package com.roovies.concertreservation.points.application.dto.result;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChargePointResult(
        Long userId,
        long totalAmount,
        LocalDateTime updatedAt
) {}
