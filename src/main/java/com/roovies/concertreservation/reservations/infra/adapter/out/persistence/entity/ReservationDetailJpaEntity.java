package com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallSeatJpaEntity;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertScheduleJpaEntity;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Getter
@Table(
        name = "reservation_details",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"schedule_id", "seat_id"})
        }
)
public class ReservationDetailJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private ReservationJpaEntity reservation;

    // 공연 스케줄 참조 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private ConcertScheduleJpaEntity schedule;

    // 좌석 참조 (FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private ConcertHallSeatJpaEntity seat;

}
