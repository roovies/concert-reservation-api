package com.roovies.concertreservation.concerts.application.port.out;

import com.roovies.concertreservation.concerts.domain.entity.Concert;

import java.util.Optional;

public interface ConcertRepositoryPort {
    Optional<Concert> findById(Long id);
    Optional<Concert> findByIdWithSchedules(Long id);
}
