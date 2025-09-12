package com.roovies.concertreservation.concerts.application.service;

import com.roovies.concertreservation.concerts.application.dto.result.GetConcertByIdResult;
import com.roovies.concertreservation.concerts.application.port.in.GetConcertByIdUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertHallQueryPort;
import com.roovies.concertreservation.concerts.application.port.out.ConcertRepositoryPort;
import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.domain.vo.external.ConcertHallSnapShot;
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
    public GetConcertByIdResult execute(Long concertId) {
        // NOTE: Controller에서 검증하고, 테스트 코드는 Controller에서 다뤄도 괜찮을 것 같음
        if (concertId == null || concertId < 1L)
            throw new IllegalArgumentException("유효하지 않은 콘서트ID입니다.");

        Concert concert = concertRepositoryPort.findByIdWithSchedules(concertId)
                .orElseThrow(() -> new NoSuchElementException("콘서트를 찾을 수 없습니다."));

        Long concertHallId = concert.getSchedule(concert.getStartDate()).getConcertHallId();
        ConcertHallSnapShot concertHall = concertHallQueryPort.findConcertHallById(concertHallId);
        LocalDate now = LocalDate.now(clock);

        return GetConcertByIdResult.from(concert, concert.getStatus(now), concertHall);
    }
}
