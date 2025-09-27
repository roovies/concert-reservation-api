package com.roovies.concertreservation.venues.application.dto.result;

import com.roovies.concertreservation.venues.domain.enums.SeatType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GetVenueWithSeatListResult(
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
}