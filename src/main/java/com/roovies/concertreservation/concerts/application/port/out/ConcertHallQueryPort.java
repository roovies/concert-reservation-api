package com.roovies.concertreservation.concerts.application.port.out;

import com.roovies.concertreservation.concerts.domain.vo.external.ConcertHallSnapShot;

public interface ConcertHallQueryPort {
    ConcertHallSnapShot findConcertHallById(Long concertHallId);
}
