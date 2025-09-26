package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.external.ExternalVenue;

import java.util.List;

public interface ReservationVenueGatewayPort {
    ExternalVenue findVenueWithSeats(Long venueId);
    Long getTotalSeatPrice(List<Long> seatIds);
}
