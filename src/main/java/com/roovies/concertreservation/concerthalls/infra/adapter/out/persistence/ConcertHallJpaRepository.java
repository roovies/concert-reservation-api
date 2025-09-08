package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConcertHallJpaRepository extends JpaRepository<ConcertHallJpaEntity, Long> {
}
