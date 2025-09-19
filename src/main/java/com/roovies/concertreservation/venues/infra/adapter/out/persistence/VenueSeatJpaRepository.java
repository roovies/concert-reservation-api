package com.roovies.concertreservation.venues.infra.adapter.out.persistence;

import com.roovies.concertreservation.venues.infra.adapter.out.persistence.entity.VenueSeatJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VenueSeatJpaRepository extends JpaRepository<VenueSeatJpaEntity, Long> {
    List<VenueSeatJpaEntity> findByVenueId(Long venueId);

    @Query("SELECT COALESCE(SUM(vs.price), 0) FROM VenueSeatJpaEntity vs WHERE vs.id IN :seatIds")
    Long getTotalPriceBySeatIds(List<Long> seatIds);
}
