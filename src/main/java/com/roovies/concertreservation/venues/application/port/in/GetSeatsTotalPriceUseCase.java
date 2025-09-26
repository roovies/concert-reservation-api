package com.roovies.concertreservation.venues.application.port.in;

import java.util.List;

public interface GetSeatsTotalPriceUseCase {
    Long getSeatListTotalPrice(List<Long> seatIds);
}
