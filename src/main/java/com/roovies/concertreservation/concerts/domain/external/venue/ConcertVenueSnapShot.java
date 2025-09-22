package com.roovies.concertreservation.concerts.domain.external.venue;

import lombok.Builder;

@Builder
public record ConcertVenueSnapShot(
        Long id,
        String name,
        int totalSeats
) {
}
