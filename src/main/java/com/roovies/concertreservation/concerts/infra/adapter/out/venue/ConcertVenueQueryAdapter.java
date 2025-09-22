package com.roovies.concertreservation.concerts.infra.adapter.out.venue;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertVenueQueryPort;
import com.roovies.concertreservation.concerts.domain.external.venue.ConcertVenueSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertVenueQueryAdapter implements ConcertVenueQueryPort {

    private final GetVenueUseCase getVenueUseCase;

    @Override
    public ConcertVenueSnapShot findVenueById(Long venueId) {
        GetVenueResult result = getVenueUseCase.execute(venueId);
        return ConcertVenueSnapShot.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .build();
    }
}
