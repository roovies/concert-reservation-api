package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Table(name = "concert_halls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertHallJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 공연장ID

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "concertHall", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConcertHallSeatJpaEntity> seats;
}
