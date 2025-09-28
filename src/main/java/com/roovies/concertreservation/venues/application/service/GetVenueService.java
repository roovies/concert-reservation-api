package com.roovies.concertreservation.venues.application.service;

import com.roovies.concertreservation.venues.application.dto.result.GetVenueResult;
import com.roovies.concertreservation.venues.application.port.in.GetVenueUseCase;
import com.roovies.concertreservation.venues.application.port.out.VenueRepositoryPort;
import com.roovies.concertreservation.venues.domain.entity.Venue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 공연장 조회 서비스 구현체.
 * <p>
 * 공연장 ID를 기반으로 공연장 정보를 조회하여 반환한다.
 * 공연장이 존재하지 않을 경우 예외를 발생시킨다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetVenueService implements GetVenueUseCase {

    private final VenueRepositoryPort venueRepositoryPort;

    @Override
    public GetVenueResult findById(Long venueId) {
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
