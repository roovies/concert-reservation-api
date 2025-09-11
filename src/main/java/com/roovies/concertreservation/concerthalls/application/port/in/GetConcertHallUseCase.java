package com.roovies.concertreservation.concerthalls.application.port.in;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallResult;

public interface GetConcertHallUseCase {
    GetConcertHallResult execute(Long concertHallId);
}
