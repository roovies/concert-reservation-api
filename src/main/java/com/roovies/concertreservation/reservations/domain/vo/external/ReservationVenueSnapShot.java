package com.roovies.concertreservation.reservations.domain.vo.external;

import com.roovies.concertreservation.venues.domain.enums.SeatType;
import lombok.Builder;

import java.util.List;

@Builder
public record ReservationVenueSnapShot(
        Long id,
        String name,
        int totalSeats,
        List<VenueSeatInfo> seats
) {
    @Builder
    public record VenueSeatInfo(
            Long id,
            int row,
            int seatNumber,
            SeatType seatType,
            long price
    ) {
    }
}
