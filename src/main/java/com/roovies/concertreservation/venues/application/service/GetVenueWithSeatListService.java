package com.roovies.concertreservation.venues.application.service;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueWithSeatListResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueWithSeatListUseCase;
import com.roovies.concertreservation.venues.application.port.out.VenueRepositoryPort;
import com.roovies.concertreservation.venues.domain.entity.Venue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * 공연장 및 좌석 목록 조회 서비스 구현체.
 * <p>
 * 공연장 ID를 기반으로 공연장 정보와 해당 공연장의 좌석 목록을 함께 조회하여 반환한다.
 * 공연장이 존재하지 않을 경우 예외를 발생시킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVenueWithSeatListService implements GetVenueWithSeatListUseCase {

    private final VenueRepositoryPort venueRepositoryPort;

    @Override
    public GetVenueWithSeatListResult findByVenueId(Long venueId) {
        Venue venue = venueRepositoryPort.findByIdWithSeats(venueId)
                .orElseThrow(() -> new NoSuchElementException("공연장을 찾을 수 없습니다."));

        List<GetVenueWithSeatListResult.SeatInfo> seats = venue.getSeats().stream()
                .map(seat -> new GetVenueWithSeatListResult.SeatInfo(
                        seat.getId(),
                        seat.getRow(),
                        seat.getSeatNumber(),
                        seat.getSeatType(),
                        seat.getPrice().amount(),
                        seat.getCreatedAt()
                ))
                .toList();

        return GetVenueWithSeatListResult.builder()
                .id(venue.getId())
                .name(venue.getName())
                .totalSeats(venue.getTotalSeats())
                .createdAt(venue.getCreatedAt())
                .updatedAt(venue.getUpdatedAt())
                .seats(seats)
                .build();
    }
}
