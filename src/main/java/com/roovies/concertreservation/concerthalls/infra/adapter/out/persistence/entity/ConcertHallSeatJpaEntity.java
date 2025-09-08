package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "concert_hall_seats")
@Getter
public class ConcertHallSeatJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 좌석ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_hall_id", nullable = false)
    private ConcertHallJpaEntity concertHall;

    @Column(nullable = false)
    private int row;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private SeatType type;

    @Column(nullable = false)
    private Long price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
