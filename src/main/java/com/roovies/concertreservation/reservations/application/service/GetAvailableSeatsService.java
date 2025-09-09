package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatsResult;
import com.roovies.concertreservation.reservations.application.port.in.GetAvailableSeatsUseCase;

import java.time.LocalDate;

public class GetAvailableSeatsService implements GetAvailableSeatsUseCase {

    @Override
    public GetAvailableSeatsResult execute(Long id, LocalDate date) {
        return null;
    }
}
