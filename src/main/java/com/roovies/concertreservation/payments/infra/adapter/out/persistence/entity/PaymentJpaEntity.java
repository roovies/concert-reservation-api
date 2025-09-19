package com.roovies.concertreservation.payments.infra.adapter.out.persistence.entity;

import com.roovies.concertreservation.payments.domain.entity.Payment;
import com.roovies.concertreservation.payments.domain.enums.PaymentStatus;
import com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity.ReservationJpaEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentJpaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(name = "original_amount", nullable = false)
    private Long originalAmount;

    @Column(name = "discount_amount", nullable = false)
    private Long discountAmount;

    @Column(name = "paid_amount", nullable = false)
    private Long paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static PaymentJpaEntity from(Payment payment) {
        return new PaymentJpaEntity(
                payment.getId(),
                payment.getOriginalAmount().value(),
                payment.getDiscountAmount().value(),
                payment.getPaidAmount().value(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }

    private PaymentJpaEntity(Long id, Long originalAmount, Long discountAmount, Long paidAmount, PaymentStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.originalAmount = originalAmount;
        this.discountAmount = discountAmount;
        this.paidAmount = paidAmount;
        this.status = status;
        this.createdAt = createdAt;
    }
}
