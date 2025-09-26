package com.roovies.concertreservation.reservations.application.dto.query;

import lombok.Builder;

import java.util.List;

@Builder
public record GetHeldSeatListQuery(
        Long scheduleId,
        List<Long> seatIds,
        Long userId
) {
}
