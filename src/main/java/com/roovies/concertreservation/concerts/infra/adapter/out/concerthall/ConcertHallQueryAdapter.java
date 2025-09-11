package com.roovies.concertreservation.concerts.infra.adapter.out.concerthall;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallResult;
import com.roovies.concertreservation.concerthalls.application.port.in.GetConcertHallUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertHallQueryPort;
import com.roovies.concertreservation.concerts.domain.vo.external.ConcertHallSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertHallQueryAdapter implements ConcertHallQueryPort {

    private final GetConcertHallUseCase getConcertHallUseCase;

    @Override
    public ConcertHallSnapShot findConcertHallById(Long concertHallId) {
        GetConcertHallResult result = getConcertHallUseCase.execute(concertHallId);
        return ConcertHallSnapShot.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .build();
    }
}
