package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.vo.external.ReservationVenueSnapShot;

import java.util.List;

public interface ReservationVenueQueryPort {
    ReservationVenueSnapShot findVenueWithSeats(Long venueId);
    Long getTotalSeatPrice(List<Long> seatIds);
}
