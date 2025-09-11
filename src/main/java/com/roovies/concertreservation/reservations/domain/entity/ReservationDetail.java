package com.roovies.concertreservation.reservations.domain.entity;

import lombok.Getter;

@Getter
public class ReservationDetail {
    // 기본 정보
    private final Long id;
    private final Long reservationId;
    private final Long scheduleId;
    private final Long seatId;

    public static ReservationDetail create(Long id, Long reservationId, Long scheduleId, Long seatId) {
        return new ReservationDetail(id, reservationId, scheduleId, seatId);
    }

    private ReservationDetail(Long id, Long reservationId, Long scheduleId, Long seatId) {
        this.id = id;
        this.reservationId = reservationId;
        this.scheduleId = scheduleId;
        this.seatId = seatId;
    }
}
