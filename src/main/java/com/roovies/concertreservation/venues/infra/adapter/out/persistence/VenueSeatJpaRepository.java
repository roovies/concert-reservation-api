package com.roovies.concertreservation.venues.infra.adapter.out.persistence;

import com.roovies.concertreservation.venues.infra.adapter.out.persistence.entity.VenueSeatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VenueSeatJpaRepository extends JpaRepository<VenueSeatJpaEntity, Long> {
    List<VenueSeatJpaEntity> findByVenueId(Long venueId);
}
