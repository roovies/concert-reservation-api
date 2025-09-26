package com.roovies.concertreservation.reservations.application.dto.query;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record GetAvailableSeatListQuery(
        Long concertId,
        LocalDate date
) {
}
