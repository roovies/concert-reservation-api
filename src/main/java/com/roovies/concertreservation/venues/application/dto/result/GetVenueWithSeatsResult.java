package com.roovies.concertreservation.venues.application.dto.result;

import com.roovies.concertreservation.venues.domain.entity.Venue;
import com.roovies.concertreservation.venues.domain.enums.SeatType;

import java.time.LocalDateTime;
import java.util.List;

public record GetVenueWithSeatsResult(
        Long id,
        String name,
        int totalSeats,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SeatInfo> seats
) {
    public record SeatInfo(
            Long id,
            int row,
            int seatNumber,
            SeatType seatType,
            long price,
            LocalDateTime createdAt
    ) {}

    public static GetVenueWithSeatsResult from(Venue venue) {
        List<SeatInfo> seats = venue.getSeats().stream()
                .map(seat -> new SeatInfo(
                        seat.getId(),
                        seat.getRow(),
                        seat.getSeatNumber(),
                        seat.getSeatType(),
                        seat.getPrice().amount(),
                        seat.getCreatedAt()
                ))
                .toList();

        return new GetVenueWithSeatsResult(
                venue.getId(),
                venue.getName(),
                venue.getTotalSeats(),
                venue.getCreatedAt(),
                venue.getUpdatedAt(),
                seats
        );
    }
}