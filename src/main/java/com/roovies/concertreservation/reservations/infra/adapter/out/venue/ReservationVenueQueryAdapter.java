package com.roovies.concertreservation.reservations.infra.adapter.out.venue;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueWithSeatsResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueWithSeatsUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueQueryPort;
import com.roovies.concertreservation.reservations.domain.vo.external.ReservationVenueSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationVenueQueryAdapter implements ReservationVenueQueryPort {

    private final GetVenueWithSeatsUseCase getVenueWithSeatsUseCase;

    @Override
    public ReservationVenueSnapShot findVenueWithSeats(Long venueId) {
        GetVenueWithSeatsResult result = getVenueWithSeatsUseCase.execute(venueId);
        List<ReservationVenueSnapShot.VenueSeatInfo> seats = result.seats().stream()
                .map(seat -> ReservationVenueSnapShot.VenueSeatInfo.builder()
                        .id(seat.id())
                        .row(seat.row())
                        .seatNumber(seat.seatNumber())
                        .seatType(seat.seatType())
                        .price(seat.price())
                        .build()
                )
                .toList();

        return ReservationVenueSnapShot.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .seats(seats)
                .build();
    }
}
