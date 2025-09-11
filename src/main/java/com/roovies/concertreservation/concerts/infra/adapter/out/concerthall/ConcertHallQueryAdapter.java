package com.roovies.concertreservation.concerts.infra.adapter.out.concerthall;

import com.roovies.concertreservation.concerthalls.application.dto.result.GetConcertHallResult;
import com.roovies.concertreservation.concerthalls.application.port.in.GetConcertHallUseCase;
import com.roovies.concertreservation.concerts.application.port.out.ConcertHallQueryPort;
import com.roovies.concertreservation.concerts.domain.vo.external.ConcertHallInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConcertHallQueryAdapter implements ConcertHallQueryPort {

    private final GetConcertHallUseCase getConcertHallUseCase;

    @Override
    public ConcertHallInfo getConcertHallInfo(Long id) {
        GetConcertHallResult result = getConcertHallUseCase.execute(id);
        return ConcertHallInfo.builder()
                .id(result.id())
                .name(result.name())
                .totalSeats(result.totalSeats())
                .build();
    }
}
