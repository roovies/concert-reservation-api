package com.roovies.concertreservation.reservations.infra.adapter.out.persistence;

import com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {
}
