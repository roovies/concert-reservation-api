package com.roovies.concertreservation.concerthalls.application.port.in;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallWithSeatsResult;

public interface GetConcertHallWithSeatsUseCase {
    GetConcertHallWithSeatsResult execute(Long concertHallId);
}
