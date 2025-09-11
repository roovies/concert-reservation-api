package com.roovies.concertreservation.concerts.application.service;

import com.roovies.concertreservation.concerts.application.dto.result.GetConcertResult;
import com.roovies.concertreservation.concerts.application.port.in.GetConcertByIdUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertHallQueryPort;
import com.roovies.concertreservation.concerts.application.port.out.ConcertRepositoryPort;
import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.domain.vo.external.ConcertHallInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetConcertByIdService implements GetConcertByIdUseCase {

    private final ConcertRepositoryPort concertRepositoryPort;
    private final ConcertHallQueryPort concertHallQueryPort;
    private final Clock clock; // LocalDate.now()를 Mocking으로 테스팅하기 위함

    @Override
    public GetConcertResult execute(Long id) {
        // MEMO: Controller에서 검증하고, 테스트 코드는 Controller에서 다뤄도 괜찮을 것 같음
        if (id == null || id < 1L)
            throw new IllegalArgumentException("유효하지 않은 콘서트ID입니다.");

        Concert concert = concertRepositoryPort.findByIdWithSchedules(id)
                .orElseThrow(() -> new NoSuchElementException("콘서트를 찾을 수 없습니다."));

        Long concertHallId = concert.getSchedule(concert.getStartDate()).getConcertHallId();
        ConcertHallInfo concertHall = concertHallQueryPort.getConcertHallInfo(concertHallId);
        LocalDate now = LocalDate.now(clock);

        return GetConcertResult.builder()
                .id(concert.getId())
                .title(concert.getTitle())
                .description(concert.getDescription())
                .minPrice(concert.getMinPrice())
                .startDate(concert.getStartDate())
                .endDate(concert.getEndDate())
                .status(concert.getStatus(now))
                .concertHallName(concertHall.name())
                .createdAt(concert.getCreatedAt())
                .updatedAt(concert.getUpdatedAt())
                .build();
    }
}
