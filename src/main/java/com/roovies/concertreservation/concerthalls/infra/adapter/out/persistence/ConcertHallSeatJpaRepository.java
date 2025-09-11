package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallSeatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConcertHallSeatJpaRepository extends JpaRepository<ConcertHallSeatJpaEntity, Long> {
    List<ConcertHallSeatJpaEntity> findByConcertHallId(Long concertHallId);
}
