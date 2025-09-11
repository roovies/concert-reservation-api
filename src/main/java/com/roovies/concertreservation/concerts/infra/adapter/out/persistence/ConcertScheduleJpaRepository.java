package com.roovies.concertreservation.concerts.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertScheduleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertScheduleJpaRepository extends JpaRepository<ConcertScheduleJpaEntity, Long> {
    List<ConcertScheduleJpaEntity> findByConcertId(Long concertId);
}
