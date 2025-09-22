package com.roovies.concertreservation.venues.application.service;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueWithSeatsResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueWithSeatsUseCase;
import com.roovies.concertreservation.venues.application.port.out.VenueRepositoryPort;
import com.roovies.concertreservation.venues.domain.entity.Venue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetVenueWithSeatsService implements GetVenueWithSeatsUseCase {

    private final VenueRepositoryPort venueRepositoryPort;

    @Override
    public GetVenueWithSeatsResult execute(Long venueId) {
        Venue venue = venueRepositoryPort.findByIdWithSeats(venueId)
                .orElseThrow(() -> new NoSuchElementException("공연장을 찾을 수 없습니다."));

        List<GetVenueWithSeatsResult.SeatInfo> seats = venue.getSeats().stream()
                .map(seat -> new GetVenueWithSeatsResult.SeatInfo(
                        seat.getId(),
                        seat.getRow(),
                        seat.getSeatNumber(),
                        seat.getSeatType(),
                        seat.getPrice().amount(),
                        seat.getCreatedAt()
                ))
                .toList();

        return GetVenueWithSeatsResult.builder()
                .id(venue.getId())
                .name(venue.getName())
                .totalSeats(venue.getTotalSeats())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .seats(seats)
                .build();
    }
}
