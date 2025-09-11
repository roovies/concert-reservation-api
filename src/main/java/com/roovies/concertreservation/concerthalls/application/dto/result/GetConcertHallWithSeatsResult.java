package com.roovies.concertreservation.concerthalls.application.dto.result;

import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record GetConcertHallWithSeatsResult(
        Long id,
        String name,
        int totalSeats,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<SeatInfo> seats


) {
    @Builder
    public record SeatInfo(
       Long id,
       int row,
       int seatNumber,
       SeatType seatType,
       long price,
       LocalDateTime createdAt
    ){}
}
