package com.roovies.concertreservation.reservations.domain.vo.external;

import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;
import lombok.Builder;

import java.util.List;

@Builder
public record ConcertHallInfo(
        Long id,
        String name,
        int totalSeats,
        List<ConcertHallSeatInfo> seats
) {
    @Builder
    public record ConcertHallSeatInfo(
            Long id,
            int row,
            int seatNumber,
            SeatType seatType,
            long price
    ) {
    }
}
