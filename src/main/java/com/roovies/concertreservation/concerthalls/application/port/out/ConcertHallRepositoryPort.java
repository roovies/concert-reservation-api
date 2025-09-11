package com.roovies.concertreservation.concerthalls.application.port.out;

import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;

import java.util.Optional;

public interface ConcertHallRepositoryPort {
    Optional<ConcertHall> findById(Long id);
}
