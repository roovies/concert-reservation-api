package com.roovies.concertreservation.points.application.dto.result;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record DeductPointResult(
        Long userId,
        long resultAmount,
        LocalDateTime updatedAt
) {
}
