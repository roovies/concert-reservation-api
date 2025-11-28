package com.roovies.concertreservation.ranking.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertScheduleJpaEntity;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 랭킹 조회를 위한 Spring Data JPA 레포지토리.
 * <p>
 * Query 어노테이션을 사용하여 복잡한 조인 쿼리를 수행한다.
 */
@Repository
public interface RankingQueryRepository extends JpaRepository<ConcertScheduleJpaEntity, Long> {

    /**
     * 주간 결제 건수를 기준으로 상위 5개 스케줄을 조회한다.
     * <p>
     * Reservation(CONFIRMED) - ReservationDetail - ConcertSchedule - Concert를 조인하여
     * 스케줄별 결제 건수(paymentId 개수)를 집계한다.
     *
     * @param status    예약 상태 (CONFIRMED)
     * @param startDate 조회 시작일시
     * @param endDate   조회 종료일시
     * @return 주간 랭킹 원시 데이터 (scheduleId, concertId, concertTitle, paymentCount)
     */
    @Query("""
            SELECT rd.scheduleId, cs.concert.id, cs.concert.title, COUNT(DISTINCT r.paymentId) AS paymentCount
            FROM ReservationJpaEntity r
            JOIN ReservationDetailJpaEntity rd ON r.id = rd.reservation.id
            JOIN ConcertScheduleJpaEntity cs ON rd.scheduleId = cs.id
            JOIN ConcertJpaEntity c ON cs.concert.id = c.id
            WHERE r.status = :status
              AND r.createdAt BETWEEN :startDate AND :endDate
            GROUP BY rd.scheduleId, cs.concert.id, cs.concert.title
            ORDER BY paymentCount DESC
            """)
    List<Object[]> findWeeklyTop5RawData(
            @Param("status") ReservationStatus status,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 스케줄 ID로 콘서트 정보를 조회한다.
     *
     * @param scheduleId 스케줄 ID
     * @return 콘서트 정보 (scheduleId, concertId, concertTitle)
     */
    @Query("""
            SELECT cs.id, cs.concert.id, cs.concert.title
            FROM ConcertScheduleJpaEntity cs
            WHERE cs.id = :scheduleId
            """)
    Optional<Object[]> findConcertInfoByScheduleIdRaw(@Param("scheduleId") Long scheduleId);
}