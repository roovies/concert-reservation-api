package com.roovies.concertreservation.reservations.application.dto.result;

import com.roovies.concertreservation.venues.domain.enums.SeatType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record GetAvailableSeatListResult(
        Long concertId,
        Long concertScheduleId,
        LocalDate date,
        List<SeatItem> availableSeats,
        boolean isAllReserved
) {
    @Builder
    public record SeatItem(
            Long seatId,
            int row,
            int seatNumber,
            SeatType seatType,
            long price
    ) {
    }
}
