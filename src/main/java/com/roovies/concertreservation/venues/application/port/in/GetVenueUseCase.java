package com.roovies.concertreservation.venues.application.port.in;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueResult;

public interface GetVenueUseCase {
    GetVenueResult execute(Long venueId);
}
