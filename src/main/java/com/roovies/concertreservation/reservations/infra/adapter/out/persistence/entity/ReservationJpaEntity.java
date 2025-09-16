package com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import com.roovies.concertreservation.users.infra.adapter.out.persistence.entity.UserJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserJpaEntity user; // 예약자 (FK)

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

    // 결제 정보 (1:1)
//    @OneToOne(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
//    private PaymentJpaEntity payment;

//    public void addReservationDetail(ReservationDetailJpaEntity detail) {
//        reservationDetails.add(detail);
//        detail.setReservation(this);
//    }
//
//    public void setPayment(Payment payment) {
//        this.payment = payment;
//        payment.setReservation(this);
//    }
}
