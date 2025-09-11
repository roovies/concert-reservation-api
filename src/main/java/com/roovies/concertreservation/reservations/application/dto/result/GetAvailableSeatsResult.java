package com.roovies.concertreservation.reservations.application.dto.result;

import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record GetAvailableSeatsResult(
        Long concertId,
        LocalDate date,
        List<SeatInfo> availableSeats,
        boolean isAllReserved
) {
    @Builder
    public record SeatInfo(
            Long seatId,
            int row,
            int seatNumber,
            SeatType seatType,
            long price
    ) {
    }
}
