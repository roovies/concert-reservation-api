package com.roovies.concertreservation.concerts.domain.entity;

import com.roovies.concertreservation.concerts.domain.enums.ScheduleStatus;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class ConcertSchedule {
    private final Long id;
    private final Long concertId;
    private final LocalDate date;
    private final int totalSeats;
    private int availableSeats;
    private ScheduleStatus scheduleStatus;

    // 다른 Aggregate Root 참조
    private final Long venueId;

    public static ConcertSchedule create(
            Long id, Long concertId,
            LocalDate scheduleDate,
            int totalSeats, int availableSeats,
            ScheduleStatus scheduleStatus, Long venueId
    ) {
        return new ConcertSchedule(id, concertId, scheduleDate, totalSeats, availableSeats, scheduleStatus, venueId);
    }

    private ConcertSchedule(
            Long id, Long concertId,
            LocalDate date,
            int totalSeats, int availableSeats,
            ScheduleStatus scheduleStatus, Long venueId
    ) {
        // availableSeats > totalSeats인지 검증
        if (availableSeats > totalSeats)
            throw new IllegalArgumentException("예약 가능한 좌석의 수는 전체 좌석 수보다 많을 수 없습니다.");

        this.id = id;
        this.concertId = concertId;
        this.date = date;
        this.totalSeats = totalSeats;
        this.availableSeats = availableSeats;
        this.scheduleStatus = scheduleStatus;
        this.venueId = venueId;
    }

    public int decreaseAvailableSeats(int count) {
        int decreased = availableSeats - count;
        if (decreased < 0)
            throw new IllegalArgumentException("잔여 좌석의 수보다 예약하는 좌석의 수가 더 클 수 없습니다.");

        if (decreased == 0)
            this.scheduleStatus = ScheduleStatus.SOLD_OUT;

        availableSeats = decreased;
        return availableSeats;
    }
}
