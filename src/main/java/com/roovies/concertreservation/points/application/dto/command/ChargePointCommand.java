package com.roovies.concertreservation.points.application.dto.command;

import lombok.Builder;

@Builder
public record ChargePointCommand(
        Long userId,
        long amount
) {}
