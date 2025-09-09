package com.roovies.concertreservation.reservations.domain.entity;

import com.roovies.concertreservation.reservations.domain.enums.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
/* Aggregate Root */
public class Reservation {
    // 기본 정보
    private final Long id;
    private final Long userId;
    private PaymentStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Child Entity
    private final List<ReservationDetail> details;

    public static Reservation create(Long id, Long userId, PaymentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, List<ReservationDetail> details) {
        return new Reservation(id, userId, status, createdAt, updatedAt, details);
    }

    private Reservation(Long id, Long userId, PaymentStatus status, LocalDateTime createdAt, LocalDateTime updatedAt, List<ReservationDetail> details) {
        if (details == null || details.isEmpty())
            throw new IllegalArgumentException("예약 상세내역은 비어있을 수 없습니다.");

        if (validateDetails(details))
            throw new IllegalArgumentException("현재 예약에 대한 예약 상세 정보만 등록할 수 있습니다.");

        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.details = details;
    }


    private boolean validateDetails(List<ReservationDetail> details) {
        return details.stream()
                .anyMatch(detail -> !detail.getReservationId().equals(this.id));
    }

}
