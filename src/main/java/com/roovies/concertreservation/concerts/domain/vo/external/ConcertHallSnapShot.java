package com.roovies.concertreservation.concerts.domain.vo.external;

import lombok.Builder;

@Builder
public record ConcertHallSnapShot(
        Long id,
        String name,
        int totalSeats
) {
}
