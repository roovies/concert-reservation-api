package com.roovies.concertreservation.concerts.application.port.out;

import com.roovies.concertreservation.concerts.domain.entity.Concert;

import java.util.Optional;

public interface ConcertQueryRepositoryPort {
    Optional<Concert> findById(Long concertId);
    Optional<Concert> findByIdWithSchedules(Long concertId);
}
