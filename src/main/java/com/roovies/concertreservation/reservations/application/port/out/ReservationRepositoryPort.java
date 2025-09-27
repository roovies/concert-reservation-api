package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.entity.Reservation;

import java.util.List;
import java.util.Optional;

public interface ReservationRepositoryPort {
    Optional<Reservation> findById(Long reservationId);
    List<Reservation> findReservationsByDetailScheduleId(Long scheduleId);
    void save(Reservation reservation);
}
