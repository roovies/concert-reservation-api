package com.roovies.concertreservation.concerts.application.port.in;

import com.roovies.concertreservation.concerts.application.dto.result.GetConcertResult;

public interface GetConcertUseCase {
    GetConcertResult findById(Long concertId);
}
