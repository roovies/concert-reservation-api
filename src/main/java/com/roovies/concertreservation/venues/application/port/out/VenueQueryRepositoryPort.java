package com.roovies.concertreservation.venues.application.port.out;

import com.roovies.concertreservation.venues.domain.entity.Venue;

import java.util.List;
import java.util.Optional;

public interface VenueQueryRepositoryPort {
    Optional<Venue> findById(Long venueId);
    Optional<Venue> findByIdWithSeats(Long venueId);
    Long getTotalSeatsPrice(List<Long> seatIds);
}
