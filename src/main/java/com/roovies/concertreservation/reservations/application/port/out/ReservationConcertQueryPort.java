package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.vo.external.ReservationConcertScheduleSnapShot;

import java.time.LocalDate;

public interface ReservationConcertQueryPort {
    ReservationConcertScheduleSnapShot findConcertSchedule(Long concertId, LocalDate date);
}
