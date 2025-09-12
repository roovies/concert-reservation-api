package com.roovies.concertreservation.venues.infra.adapter.out.persistence;

import com.roovies.concertreservation.venues.infra.adapter.out.persistence.entity.VenueJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VenueJpaRepository extends JpaRepository<VenueJpaEntity, Long> {

    @Query("SELECT v FROM VenueJpaEntity v " +
            "LEFT JOIN FETCH v.seats seats " +
            "WHERE v.id = :venueId")
    Optional<VenueJpaEntity> findByIdWithSeats(@Param("venueId") Long venueId);
}
