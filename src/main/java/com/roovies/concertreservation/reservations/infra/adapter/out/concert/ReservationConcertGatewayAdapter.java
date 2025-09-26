package com.roovies.concertreservation.reservations.infra.adapter.out.concert;

import com.roovies.concertreservation.reservations.application.port.out.ReservationConcertGatewayPort;
import com.roovies.concertreservation.reservations.domain.external.ExternalConcertSchedule;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class ReservationConcertGatewayAdapter implements ReservationConcertGatewayPort {
    @Override
    public ExternalConcertSchedule findConcertSchedule(Long concertId, LocalDate date) {
        return null;
    }
}
