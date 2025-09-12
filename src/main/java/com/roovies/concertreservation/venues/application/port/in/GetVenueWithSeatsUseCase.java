package com.roovies.concertreservation.venues.application.port.in;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueWithSeatsResult;

public interface GetVenueWithSeatsUseCase {
    GetVenueWithSeatsResult execute(Long venueId);
}
