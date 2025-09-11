package com.roovies.concertreservation.concerts.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ConcertJpaRepository extends JpaRepository<ConcertJpaEntity, Long> {
    @Query("SELECT c FROM ConcertJpaEntity c " +
            "LEFT JOIN FETCH c.schedules s " +
            "WHERE c.id = :concertId")
    Optional<ConcertJpaEntity> findByIdWithSchedules(@Param("concertId") Long concertId);
}
