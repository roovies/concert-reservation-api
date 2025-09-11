package com.roovies.concertreservation.concerts.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertJpaRepository extends JpaRepository<ConcertJpaEntity, Long> {
}
