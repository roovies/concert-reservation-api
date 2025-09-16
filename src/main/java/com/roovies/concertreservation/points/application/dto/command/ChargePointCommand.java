package com.roovies.concertreservation.points.application.dto.command;

public record ChargePointCommand(
        Long userId,
        long amount
) {
    public static ChargePointCommand of(Long userId, long amount) {
        return new ChargePointCommand(userId, amount);
    }
}
