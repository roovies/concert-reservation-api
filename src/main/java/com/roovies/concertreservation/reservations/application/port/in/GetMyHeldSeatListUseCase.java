package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;

public interface GetMyHeldSeatListUseCase {
    HoldSeatResult getMyHeldSeatList(GetHeldSeatListQuery query);
}
