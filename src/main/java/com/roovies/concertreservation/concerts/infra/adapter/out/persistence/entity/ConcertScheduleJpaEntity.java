package com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.venues.infra.adapter.out.persistence.entity.VenueJpaEntity;
import com.roovies.concertreservation.concerts.domain.enums.ScheduleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "concert_schedules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertScheduleJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 스케줄ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private ConcertJpaEntity concert;

    /**
     * Venue는 다른 BC이므로 식별자만 저장
     * - DB FK X (물리 DB 분리 고려)
     * - ConcertSchedule → Venue 관계는 API/이벤트/ACL을 통해 연결
     */
    @Column(name = "venue_id", nullable = false)
    private Long venueId;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_status", nullable = false)
    private ScheduleStatus scheduleStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
