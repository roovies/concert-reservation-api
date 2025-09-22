package com.roovies.concertreservation.concerts.application.port.out;

import com.roovies.concertreservation.concerts.domain.external.venue.ConcertVenueSnapShot;

public interface ConcertVenueQueryPort {
    ConcertVenueSnapShot findVenueById(Long venueId);
}
