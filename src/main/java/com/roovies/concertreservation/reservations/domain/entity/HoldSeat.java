package com.roovies.concertreservation.reservations.domain.entity;

import lombok.Getter;

@Getter
public class HoldSeat {
    private final Long scheduleId;
    private final Long seatId;
    private final Long holdUserId;
    private final long remainSeconds;

    public static HoldSeat create(Long scheduleId, Long seatId, Long holdUserId,
                                  long remainSeconds) {
        return new HoldSeat(scheduleId, seatId, holdUserId, remainSeconds);
    }

    private HoldSeat(Long scheduleId, Long seatId, Long holdUserId,
                     long remainSeconds) {
        this.scheduleId = scheduleId;
        this.seatId = seatId;
        this.holdUserId = holdUserId;
        this.remainSeconds = remainSeconds;
    }

    /**
     * 홀딩이 만료되었는지 확인
     */
    public boolean isExpired() {
        return remainSeconds <= 0;
    }

    /**
     * 홀딩이 곧 만료되는지 확인 (1분 이내)
     */
    public boolean isExpiringSoon() {
        return remainSeconds > 0 && remainSeconds <= 60;
    }
}