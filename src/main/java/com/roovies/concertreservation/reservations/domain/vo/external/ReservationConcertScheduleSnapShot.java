package com.roovies.concertreservation.reservations.domain.vo.external;

import com.roovies.concertreservation.concerts.domain.enums.ReservationStatus;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record ReservationConcertScheduleSnapShot(
        Long id,
        LocalDate date,
        int availableSeats,
        ReservationStatus status,
        Long venueId
) {

    public boolean isSoldOut() {
        return availableSeats == 0 || status.equals(ReservationStatus.SOLD_OUT);
    }
}
