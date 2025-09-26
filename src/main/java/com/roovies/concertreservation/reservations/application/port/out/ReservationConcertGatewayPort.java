package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.external.ExternalConcertSchedule;

import java.time.LocalDate;

public interface ReservationConcertGatewayPort {
    ExternalConcertSchedule findConcertSchedule(Long concertId, LocalDate date);
}
