package com.roovies.concertreservation.points.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.users.infra.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "points")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointJpaEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // userId를 PK이자 FK로 사용
    @JoinColumn(name = "user_id")
    private UserJpaEntity user;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static PointJpaEntity from(Point point) {
        return new PointJpaEntity(
                point.getUserId(),
                point.getAmount().value(),
                point.getUpdatedAt()
        );
    }

    private PointJpaEntity(Long userId, Long amount, LocalDateTime updatedAt) {
        this.userId = userId;
        this.amount = amount;
        this.updatedAt = updatedAt;
    }
}
