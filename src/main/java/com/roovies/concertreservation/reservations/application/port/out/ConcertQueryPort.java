package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.vo.external.ConcertScheduleSnapShot;

import java.time.LocalDate;

public interface ConcertQueryPort {
    ConcertScheduleSnapShot findConcertSchedule(Long concertId, LocalDate date);
}
