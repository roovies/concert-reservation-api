package com.roovies.concertreservation.concerts.domain.vo.external;

import lombok.Builder;

@Builder
public record ConcertVenueSnapShot(
        Long id,
        String name,
        int totalSeats
) {
}
