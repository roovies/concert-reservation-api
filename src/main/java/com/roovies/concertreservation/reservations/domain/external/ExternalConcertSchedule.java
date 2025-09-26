package com.roovies.concertreservation.reservations.domain.external;

import com.roovies.concertreservation.concerts.domain.enums.ScheduleStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ExternalConcertSchedule(
        Long id,
        LocalDate date,
        int availableSeats,
        ScheduleStatus status,
        Long venueId
) {

    public boolean isSoldOut() {
        return availableSeats == 0 || status.equals(ScheduleStatus.SOLD_OUT);
    }
}
