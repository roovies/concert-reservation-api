package com.roovies.concertreservation.venues.application.service;

import com.roovies.concertreservation.venues.application.port.in.GetSeatsTotalPriceUseCase;
import com.roovies.concertreservation.venues.application.port.out.VenueRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSeatsTotalPriceService implements GetSeatsTotalPriceUseCase {

    private final VenueRepositoryPort venueRepositoryPort;

    @Override
    public Long getSeatListTotalPrice(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0L;
        }

        return venueRepositoryPort.getTotalSeatsPrice(seatIds);
    }
}
