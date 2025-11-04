package com.roovies.concertreservation.venues.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.venues.domain.enums.SeatType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "venue_seats")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VenueSeatJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 좌석ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private VenueJpaEntity venue;

    @Column(name = "seat_row", nullable = false)
    private int seatRow;

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
