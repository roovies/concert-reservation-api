package com.roovies.concertreservation.reservations.domain.entity;

import com.roovies.concertreservation.reservations.domain.enums.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.details = details;

        validateDetails(details);
    }

    private void validateDetails(List<ReservationDetail> details) {
        validateNotEmpty(details);
        validateDetailsReservationId(details);
        validateUniqueScheduleAndSeat(details);
    }

    private void validateNotEmpty(List<ReservationDetail> details) {
        if (details == null || details.isEmpty())
            throw new IllegalArgumentException("예약 상세내역은 비어있을 수 없습니다.");
    }

    private void validateDetailsReservationId(List<ReservationDetail> details) {
        boolean hasInvalidReservationId = details.stream()
                .anyMatch(detail -> !detail.getReservationId().equals(this.id));

        if (hasInvalidReservationId)
            throw new IllegalArgumentException("현재 예약에 대한 예약 상세내역만 등록할 수 있습니다.");
    }

    // 레코드로 키조합 표현 (equals, hashcode 자동 생성)
    // = static final inner class
    private record ScheduleAndSeat(Long scheduleId, Long seatId) {}

    private void validateUniqueScheduleAndSeat(List<ReservationDetail> details) {
        Set<ScheduleAndSeat> set = new HashSet<>();

        for (ReservationDetail detail : details) {
            ScheduleAndSeat key = new ScheduleAndSeat(detail.getScheduleId(), detail.getSeatId());
            if (!set.add(key))
                throw new IllegalArgumentException("동일한 날짜의 같은 좌석은 중복 예약할 수 없습니다.");
        }

    }

    public List<ReservationDetail> getDetails() {
        return Collections.unmodifiableList(details);
    }

}
