package com.roovies.concertreservation.reservations.application.port.in;

import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatListQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatListResult;

public interface GetAvailableSeatListUseCase {
    GetAvailableSeatListResult getAvailableSeatList(GetAvailableSeatListQuery query);

}
