package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldSeatService implements HoldSeatUseCase {

    private final HoldSeatCachePort holdSeatCachePort;

    @Override
    public HoldSeatResult execute(HoldSeatCommand command) {
        Long scheduleId = command.scheduleId();
        List<Long> seatIds = command.seatIds();
        Long userId = command.userId();

        // 1. 좌석 목록 검증
        if (seatIds == null || seatIds.isEmpty()) {
            log.warn("홀딩할 좌석 목록이 비어있음: scheduleId={}, userId={}", scheduleId, userId);
            throw new IllegalArgumentException("예약할 좌석이 없습니다.");
        }

        // 2. 좌석 ID 중복 제거
        List<Long> uniqueSeatIds = seatIds.stream()
                .distinct()
                .toList();

        // 3. 멱등성처리 - 만약 현재 사용자가 모든 좌석을 홀딩하고 있다면 이미 예약한 것
        // - 동일한 요청 시 동일한 결과를 반환
        if (holdSeatCachePort.validateHoldSeatList(scheduleId, uniqueSeatIds, userId)) {
            long ttl = holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId);
            return HoldSeatResult.of(scheduleId, uniqueSeatIds, userId, ttl);
        }

        // 4. 좌석 예약 시도
        boolean result = holdSeatCachePort.holdSeatList(scheduleId, uniqueSeatIds, userId);
        if (!result)
            throw new IllegalStateException("다른 사용자가 이미 예약 중인 좌석입니다.");

        return HoldSeatResult.of(scheduleId, uniqueSeatIds, userId, holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId));
    }
}
