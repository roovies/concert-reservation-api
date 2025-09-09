package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatsResult;

import java.time.LocalDate;

public interface GetAvailableSeatsUseCase {
    GetAvailableSeatsResult execute(Long id, LocalDate date);

}
