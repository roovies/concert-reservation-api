package com.roovies.concertreservation.concerts.application.port.out;

import com.roovies.concertreservation.concerts.domain.external.ExternalVenue;

public interface ConcertVenueGatewayPort {
    ExternalVenue findVenueById(Long venueId);
}
