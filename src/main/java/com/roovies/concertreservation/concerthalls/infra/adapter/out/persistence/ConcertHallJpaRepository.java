package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConcertHallJpaRepository extends JpaRepository<ConcertHallJpaEntity, Long> {

    @Query("SELECT hall FROM ConcertHallJpaEntity hall " +
            "LEFT JOIN FETCH hall.seats seats " +
            "WHERE hall.id = :id")
    Optional<ConcertHallJpaEntity> findByIdWithSeats(@Param("id") Long id);
}
