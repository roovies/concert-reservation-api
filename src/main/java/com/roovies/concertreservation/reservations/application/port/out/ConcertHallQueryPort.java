package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.vo.external.ConcertHallInfo;

public interface ConcertHallQueryPort {
    ConcertHallInfo getConcertHallInfo(Long concertHallId);
}
