package com.roovies.concertreservation.concerthalls.application.dto.result;

import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;
import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;

import java.time.LocalDateTime;
import java.util.List;

public record GetConcertHallWithSeatsResult(
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

    public static GetConcertHallWithSeatsResult from(ConcertHall concertHall) {
        List<SeatInfo> seats = concertHall.getSeats().stream()
                .map(seat -> new SeatInfo(
                        seat.getId(),
                        seat.getRow(),
                        seat.getSeatNumber(),
                        seat.getSeatType(),
                        seat.getPrice().amount(),
                        seat.getCreatedAt()
                ))
                .toList();

        return new GetConcertHallWithSeatsResult(
                concertHall.getId(),
                concertHall.getName(),
                concertHall.getTotalSeats(),
                concertHall.getCreatedAt(),
                concertHall.getUpdatedAt(),
                seats
        );
    }
}