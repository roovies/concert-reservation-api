package com.roovies.concertreservation.venues.application.service;

import com.roovies.concertreservation.venues.application.port.in.GetSeatsTotalPriceUseCase;
import com.roovies.concertreservation.venues.application.port.out.VenueRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 좌석 총 가격 조회를 처리하는 서비스 구현체.
 * <p>
 * 주어진 좌석 ID 목록을 기반으로 전체 금액을 계산한다.
 * 좌석 목록이 비어있거나 {@code null}일 경우 0을 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetSeatsTotalPriceService implements GetSeatsTotalPriceUseCase {

    private final VenueRepositoryPort venueRepositoryPort;

    @Override
    public Long getSeatListTotalPrice(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0L;
        }

        return venueRepositoryPort.getTotalSeatsPrice(seatIds);
    }
}
