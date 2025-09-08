package com.roovies.concertreservation.concerts.application.port.out;

import com.roovies.concertreservation.concerts.domain.vo.external.ConcertHallInfo;

public interface ConcertHallQueryPort {
    ConcertHallInfo getConcertHallInfo(Long id);
}
