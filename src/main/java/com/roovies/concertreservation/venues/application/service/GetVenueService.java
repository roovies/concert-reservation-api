package com.roovies.concertreservation.venues.application.service;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueUseCase;
import com.roovies.concertreservation.venues.application.port.out.VenueRepositoryPort;
import com.roovies.concertreservation.venues.domain.entity.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetVenueService implements GetVenueUseCase {

    private final VenueRepositoryPort venueRepositoryPort;

    @Override
    public GetVenueResult execute(Long venueId) {
        Venue venue = venueRepositoryPort.findById(venueId)
                .orElseThrow(() -> new NoSuchElementException("공연장을 찾을 수 없습니다."));
        return GetVenueResult.builder()
                .id(venue.getId())
                .name(venue.getName())
                .totalSeats(venue.getTotalSeats())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .build();
    }
}
