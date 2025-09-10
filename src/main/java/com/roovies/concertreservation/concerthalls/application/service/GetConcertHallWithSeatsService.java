package com.roovies.concertreservation.concerthalls.application.service;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallWithSeatsResult;
import com.roovies.concertreservation.concerthalls.application.port.in.GetConcertHallWithSeatsUseCase;
import com.roovies.concertreservation.concerthalls.application.port.out.ConcertHallRepositoryPort;
import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetConcertHallWithSeatsService implements GetConcertHallWithSeatsUseCase {

    private final ConcertHallRepositoryPort concertHallRepositoryPort;

    @Override
    public GetConcertHallWithSeatsResult execute(Long concertHallId) {
        ConcertHall concertHall = concertHallRepositoryPort.findByIdWithSeats(concertHallId)
                .orElseThrow(() -> new NoSuchElementException("공연장을 찾을 수 없습니다."));

        List <GetConcertHallWithSeatsResult.SeatItem> seats = concertHall.getSeats().stream()
                .map(seat -> GetConcertHallWithSeatsResult.SeatItem.builder()
                        .id(seat.getId())
                        .row(seat.getRow())
                        .seatNumber(seat.getSeatNumber())
                        .seatType(seat.getSeatType())
                        .price(seat.getPrice().amount())
                        .createdAt(seat.getCreatedAt())
                        .build()
                )
                .toList();

        return GetConcertHallWithSeatsResult.builder()
                .id(concertHall.getId())
                .name(concertHall.getName())
                .totalSeats(concertHall.getTotalSeats())
                .createdAt(concertHall.getCreatedAt())
                .updatedAt(concertHall.getUpdatedAt())
                .seats(seats)
                .build();
    }
}
