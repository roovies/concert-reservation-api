package com.roovies.concertreservation.concerts.application.service.query;

import com.roovies.concertreservation.concerts.application.dto.result.GetConcertResult;
import com.roovies.concertreservation.concerts.application.port.in.GetConcertUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertVenueGatewayPort;
import com.roovies.concertreservation.concerts.application.port.out.ConcertQueryRepositoryPort;
import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.domain.external.ExternalVenue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetConcertService implements GetConcertUseCase {

    private final ConcertQueryRepositoryPort concertQueryRepositoryPort;
    private final ConcertVenueGatewayPort concertVenueGatewayPort;
    private final Clock clock; // LocalDate.now()를 Mocking으로 테스팅하기 위함

    @Override
    public GetConcertResult findById(Long concertId) {
        // NOTE: Controller에서 검증하고, 테스트 코드는 Controller에서 다뤄도 괜찮을 것 같음
        if (concertId == null || concertId < 1L)
            throw new IllegalArgumentException("유효하지 않은 콘서트ID입니다.");

        Concert concert = concertQueryRepositoryPort.findByIdWithSchedules(concertId)
                .orElseThrow(() -> new NoSuchElementException("콘서트를 찾을 수 없습니다."));

        Long venueId = concert.getSchedule(concert.getStartDate()).getVenueId();
        ExternalVenue venue = concertVenueGatewayPort.findVenueById(venueId);
        LocalDate now = LocalDate.now(clock);

        return GetConcertResult.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .description(concert.getDescription())
                .minPrice(concert.getMinPrice())
                .startDate(concert.getStartDate())
                .endDate(concert.getEndDate())
                .status(concert.getStatus(now))
                .venueName(venue.name())
                .createdAt(concert.getCreatedAt())
                .updatedAt(concert.getUpdatedAt())
                .build();
    }
}
