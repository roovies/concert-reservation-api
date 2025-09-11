package com.roovies.concertreservation.reservations.infra.adapter.out.concert;

import com.roovies.concertreservation.reservations.application.port.out.ConcertQueryPort;
import com.roovies.concertreservation.reservations.domain.vo.external.ConcertScheduleSnapShot;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ConcertQueryAdapter implements ConcertQueryPort {
    @Override
    public ConcertScheduleSnapShot findConcertSchedule(Long concertId, LocalDate date) {
        return null;
    }
}
