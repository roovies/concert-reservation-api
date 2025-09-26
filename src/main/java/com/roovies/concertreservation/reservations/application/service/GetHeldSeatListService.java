package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.query.GetHeldSeatListQuery;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.GetHeldSeatListUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueGatewayPort;
import com.roovies.concertreservation.reservations.domain.entity.HoldSeat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetHeldSeatListService implements GetHeldSeatListUseCase {

    private final HoldSeatCachePort holdSeatCachePort;
    private final ReservationVenueGatewayPort reservationVenueGatewayPort;

    @Override
    public HoldSeatResult getHeldSeatList(GetHeldSeatListQuery query) {
        List<HoldSeat> response = holdSeatCachePort.getHoldSeatList(query.scheduleId(), query.seatIds(), query.userId());
        if (response.isEmpty())
            return HoldSeatResult.builder()
                    .scheduleId(query.scheduleId())
                    .seatIds(Collections.emptyList())
                    .userId(query.userId())
                    .totalPrice(0L)
                    .ttlSeconds(0L)
                    .build();

        long ttl = holdSeatCachePort.getHoldTTLSeconds(query.scheduleId(), query.seatIds(), query.userId());
        List<Long> seatIds = response.stream()
                .map(holdSeat -> holdSeat.getSeatId())
                .toList();
        Long scheduleId = response.get(0).getScheduleId();
        Long totalPrice = reservationVenueGatewayPort.getTotalSeatPrice(seatIds);

        return HoldSeatResult.builder()
                .scheduleId(scheduleId)
                .seatIds(seatIds)
                .userId(query.userId())
                .totalPrice(totalPrice)
                .ttlSeconds(ttl)
                .build();
    }
}
