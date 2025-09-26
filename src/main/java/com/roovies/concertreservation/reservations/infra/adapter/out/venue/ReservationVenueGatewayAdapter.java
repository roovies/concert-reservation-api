package com.roovies.concertreservation.reservations.infra.adapter.out.venue;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueWithSeatListResult;
import com.roovies.concertreservation.venues.application.port.in.GetSeatsTotalPriceUseCase;
import com.roovies.concertreservation.venues.application.port.in.GetVenueWithSeatListUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueGatewayPort;
import com.roovies.concertreservation.reservations.domain.external.ExternalVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationVenueGatewayAdapter implements ReservationVenueGatewayPort {

    private final GetVenueWithSeatListUseCase getVenueWithSeatListUseCase;
    private final GetSeatsTotalPriceUseCase getSeatsTotalPriceUseCase;

    @Override
    public ExternalVenue findVenueWithSeats(Long venueId) {
        GetVenueWithSeatListResult result = getVenueWithSeatListUseCase.findByVenueId(venueId);
        List<ExternalVenue.SeatItem> seats = result.seats().stream()
                .map(seat -> ExternalVenue.SeatItem.builder()
                        .id(seat.id())
                        .row(seat.row())
                        .seatNumber(seat.seatNumber())
                        .seatType(seat.seatType())
                        .price(seat.price())
                        .build()
                )
                .toList();

        return ExternalVenue.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .seats(seats)
                .build();
    }

    @Override
    public Long getTotalSeatPrice(List<Long> seatIds) {
        return getSeatsTotalPriceUseCase.getSeatListTotalPrice(seatIds);
    }
}
