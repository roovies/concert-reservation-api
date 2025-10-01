package com.roovies.concertreservation.points.application.dto.command;

import lombok.Builder;

@Builder
public record DeductPointCommand(
        Long userId,
        long amount
) {
}
