package com.roovies.concertreservation.reservations.infra.adapter.out.persistence;

import com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity.ReservationJpaEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReservationJpaRepository extends JpaRepository<ReservationJpaEntity, Long> {

    // user를 함께 fetch하여 N+1 방지
    @EntityGraph(attributePaths = {"user"})
    Optional<ReservationJpaEntity> findById(Long id);

    @Query("SELECT DISTINCT r FROM ReservationJpaEntity r " +
            "LEFT JOIN FETCH r.reservationDetails rd " +
            "LEFT JOIN FETCH r.user " +
            "WHERE r.id IN (SELECT DISTINCT rd2.reservation.id FROM ReservationDetailJpaEntity rd2 " +
            "WHERE rd2.schedule.id = :scheduleId)")
    List<ReservationJpaEntity> findByDetailScheduleIdWithDetails(@Param("scheduleId") Long scheduleId);
}
