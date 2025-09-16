package com.roovies.concertreservation.points.infra.adapter.out.persistence;

import com.roovies.concertreservation.points.infra.adapter.out.persistence.entity.PointJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointJpaRepository extends JpaRepository<PointJpaEntity, Long> {
}
