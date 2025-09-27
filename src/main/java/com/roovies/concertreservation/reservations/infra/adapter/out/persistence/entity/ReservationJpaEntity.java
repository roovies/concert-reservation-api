package com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Getter
@Entity
@Table(name = "reservations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Payment는 다른 BC이므로 식별자만 저장
     * - DB FK X (물리 DB 분리 고려)
     * - Reservation → Payment 관계는 API/이벤트/ACL을 통해 연결
     */
    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    /**
     * User도 다른 BC이므로 식별자만 저장
     * - DB FK X (물리 DB 분리 고려)
     * - Reservation → User 관계는 API/이벤트/ACL을 통해 연결
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus status;  // HOLD/CONFIRMED/CANCELLED/REFUNDED


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 예약 상세 (1:N)
    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReservationDetailJpaEntity> reservationDetails = new ArrayList<>();

    @Builder
    ReservationJpaEntity(Long id, Long paymentId, Long userId, ReservationStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, List<ReservationDetailJpaEntity> reservationDetails) {
        this.id = id;
        this.paymentId = paymentId;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.reservationDetails = reservationDetails;
    }
}
