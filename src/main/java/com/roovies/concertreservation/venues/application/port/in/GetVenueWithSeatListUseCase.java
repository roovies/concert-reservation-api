package com.roovies.concertreservation.venues.application.port.in;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueWithSeatListResult;

public interface GetVenueWithSeatListUseCase {
    GetVenueWithSeatListResult findByVenueId(Long venueId);
}
