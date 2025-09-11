package com.roovies.concertreservation.reservations.application.port.out;

import com.roovies.concertreservation.reservations.domain.vo.external.ConcertHallSnapShot;

public interface ConcertHallQueryPort {
    ConcertHallSnapShot findConcertHallById(Long concertHallId);
}
