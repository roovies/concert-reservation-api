package com.roovies.concertreservation.concerts.application.dto.result;

import com.roovies.concertreservation.concerts.domain.enums.ConcertStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record GetConcertResult(
        Long id,
        String title,
        String description,
        long minPrice,
        LocalDate startDate,
        LocalDate endDate,
        ConcertStatus status,
        String venueName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
