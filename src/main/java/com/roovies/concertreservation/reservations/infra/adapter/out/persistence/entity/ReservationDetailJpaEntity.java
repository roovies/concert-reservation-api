package com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "reservation_details",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"schedule_id", "seat_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationDetailJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationJpaEntity reservation;

    /**
     * Concert는 다른 BC이므로 식별자만 저장
     * - DB FK X (물리 DB 분리 고려)
     * - ReservationDetail → ConcertSchedule 관계는 API/이벤트/ACL을 통해 연결
     */
    @Column(name = "schedule_id", nullable = false)
    private Long scheduleId;

    /**
     * VenueSeat도 다른 BC이므로 식별자만 저장
     * - DB FK X (물리 DB 분리 고려)
     * - ReservationDetail → VenueSeat 관계는 API/이벤트/ACL을 통해 연결
     */
    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    @Builder
    ReservationDetailJpaEntity(Long id, ReservationJpaEntity reservation, Long scheduleId, Long seatId) {
        this.id = id;
        this.reservation = reservation;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
    }

    /**
     * 테스트용 팩토리 메서드
     */
    public static ReservationDetailJpaEntity create(ReservationJpaEntity reservation, Long scheduleId, Long seatId) {
        return ReservationDetailJpaEntity.builder()
                .reservation(reservation)
                .scheduleId(scheduleId)
                .seatId(seatId)
                .build();
    }
}