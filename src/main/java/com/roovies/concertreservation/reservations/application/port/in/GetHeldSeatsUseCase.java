package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;

public interface GetHeldSeatsUseCase {
    HoldSeatResult execute(GetHeldSeatsQuery query);
}
