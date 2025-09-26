package com.roovies.concertreservation.concerts.domain.external;

import lombok.Builder;

@Builder
public record ExternalVenue(
        Long id,
        String name,
        int totalSeats
) {
}
