package com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Entity
@Table(name = "concerts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConcertJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description; // 상세설명

    @Column(nullable = false)
    private Long minPrice; // 최상위 가격

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "concert", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConcertScheduleJpaEntity> schedules;

    public void setSchedules(List<ConcertScheduleJpaEntity> schedules) {
        this.schedules = schedules;
    }

    /**
     * 테스트용 팩토리 메서드
     */
    public static ConcertJpaEntity create(String title, String artist) {
        ConcertJpaEntity concert = new ConcertJpaEntity();
        concert.title = title;
        concert.description = artist + " 콘서트";
        concert.minPrice = 10000L;
        concert.startDate = LocalDate.now();
        concert.endDate = LocalDate.now().plusMonths(1);
        concert.createdAt = LocalDateTime.now();
        concert.updatedAt = LocalDateTime.now();
        return concert;
    }
}