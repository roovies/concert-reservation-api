package com.roovies.concertreservation.reservations.infra.adapter.out.persistence;

import com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity.ReservationDetailJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationDetailJpaRepository extends JpaRepository<ReservationDetailJpaEntity, Long> {
}
