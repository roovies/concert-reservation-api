package com.roovies.concertreservation.reservations.infra.adapter.out.concerthall;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallWithSeatsResult;
import com.roovies.concertreservation.concerthalls.application.port.in.GetConcertHallWithSeatsUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ConcertHallQueryPort;
import com.roovies.concertreservation.reservations.domain.vo.external.ConcertHallSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ConcertHallQueryAdapter implements ConcertHallQueryPort {

    private final GetConcertHallWithSeatsUseCase getConcertHallWithSeatsUseCase;

    @Override
    public ConcertHallSnapShot findConcertHallWithSeats(Long concertHallId) {
        GetConcertHallWithSeatsResult result = getConcertHallWithSeatsUseCase.execute(concertHallId);
        List<ConcertHallSnapShot.ConcertHallSeatInfo> seats = result.seats().stream()
                .map(seat -> ConcertHallSnapShot.ConcertHallSeatInfo.builder()
                        .id(seat.id())
                        .row(seat.row())
                        .seatNumber(seat.seatNumber())
                        .seatType(seat.seatType())
                        .price(seat.price())
                        .build()
                )
                .toList();

        return ConcertHallSnapShot.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .seats(seats)
                .build();
    }
}
