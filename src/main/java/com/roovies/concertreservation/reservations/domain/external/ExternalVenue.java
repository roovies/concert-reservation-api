package com.roovies.concertreservation.reservations.domain.external;

import com.roovies.concertreservation.venues.domain.enums.SeatType;
import lombok.Builder;

import java.util.List;

@Builder
public record ExternalVenue(
        Long id,
        String name,
        int totalSeats,
        List<SeatItem> seats
) {
    @Builder
    public record SeatItem(
            Long id,
            int row,
            int seatNumber,
            SeatType seatType,
            long price
    ) {
    }
}
