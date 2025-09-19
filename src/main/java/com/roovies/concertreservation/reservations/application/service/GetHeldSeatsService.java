package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.GetHeldSeatsUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueQueryPort;
import com.roovies.concertreservation.reservations.domain.entity.HoldSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetHeldSeatsService implements GetHeldSeatsUseCase {

    private final HoldSeatCachePort holdSeatCachePort;
    private final ReservationVenueQueryPort reservationVenueQueryPort;

    @Override
    public HoldSeatResult execute(GetHeldSeatsQuery query) {
        List<HoldSeat> response = holdSeatCachePort.getHoldSeatList(query.scheduleId(), query.seatIds(), query.userId());
        if (response.isEmpty())
            return HoldSeatResult.of(query.scheduleId(), Collections.emptyList(), query.userId(), 0L, 0L);

        long ttl = holdSeatCachePort.getHoldTTLSeconds(query.scheduleId(), query.seatIds(), query.userId());
        List<Long> seatIds = response.stream()
                .map(holdSeat -> holdSeat.getSeatId())
                .toList();
        Long scheduleId = response.get(0).getScheduleId();
        Long totalPrice = reservationVenueQueryPort.getTotalSeatPrice(seatIds);

        return HoldSeatResult.of(scheduleId, seatIds, query.userId(), totalPrice, ttl);
    }
}
