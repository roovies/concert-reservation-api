package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatListResult;

public interface GetAvailableSeatsUseCase {
    GetAvailableSeatListResult execute(GetAvailableSeatsQuery query);

}
