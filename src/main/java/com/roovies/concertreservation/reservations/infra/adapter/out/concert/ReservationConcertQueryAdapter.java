package com.roovies.concertreservation.reservations.infra.adapter.out.concert;

import com.roovies.concertreservation.reservations.application.port.out.ReservationConcertQueryPort;
import com.roovies.concertreservation.reservations.domain.vo.external.ReservationConcertScheduleSnapShot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReservationConcertQueryAdapter implements ReservationConcertQueryPort {
    @Override
    public ReservationConcertScheduleSnapShot findConcertSchedule(Long concertId, LocalDate date) {
        return null;
    }
}
