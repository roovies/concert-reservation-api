package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.vo.external.ConcertScheduleInfo;

import java.time.LocalDate;

public interface ConcertQueryPort {
    ConcertScheduleInfo getConcertScheduleInfo(Long concertId, LocalDate date);

}
