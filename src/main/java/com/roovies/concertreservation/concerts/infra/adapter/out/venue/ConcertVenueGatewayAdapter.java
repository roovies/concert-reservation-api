package com.roovies.concertreservation.concerts.infra.adapter.out.venue;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertVenueGatewayPort;
import com.roovies.concertreservation.concerts.domain.external.ExternalVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertVenueGatewayAdapter implements ConcertVenueGatewayPort {

    private final GetVenueUseCase getVenueUseCase;

    @Override
    public ExternalVenue findVenueById(Long venueId) {
        GetVenueResult result = getVenueUseCase.findById(venueId);
        return ExternalVenue.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .build();
    }
}
