package com.roovies.concertreservation.concerthalls.application.service;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallResult;
import com.roovies.concertreservation.concerthalls.application.port.in.GetConcertHallUseCase;
import com.roovies.concertreservation.concerthalls.application.port.out.ConcertHallRepositoryPort;
import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetConcertHallService implements GetConcertHallUseCase {

    private final ConcertHallRepositoryPort concertHallRepositoryPort;


    @Override
    public GetConcertHallResult execute(Long concertHallId) {
        ConcertHall concertHall = concertHallRepositoryPort.findById(concertHallId)
                .orElseThrow(() -> new NoSuchElementException("공연장을 찾을 수 없습니다."));
        return GetConcertHallResult.builder()
                .id(concertHall.getId())
                .name(concertHall.getName())
                .totalSeats(concertHall.getTotalSeats())
                .createdAt(concertHall.getCreatedAt())
                .updatedAt(concertHall.getUpdatedAt())
                .build();
    }
}
