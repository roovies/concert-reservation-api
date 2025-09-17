package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatIdempotencyCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoldSeatService implements HoldSeatUseCase {

    private final HoldSeatCachePort holdSeatCachePort;
    private final HoldSeatIdempotencyCachePort holdSeatIdempotencyCachePort;

    @Override
    public HoldSeatResult execute(HoldSeatCommand command) {
        String idempotencyKey = command.idempotencyKey();
        Long scheduleId = command.scheduleId();
        List<Long> seatIds = command.seatIds();
        Long userId = command.userId();

        // 1. 멱등성 키 검증
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            log.warn("멱등성 키가 비어있음: scheduleId={}, userId={}", scheduleId, userId);
            throw new IllegalArgumentException("요청 식별자(Idempotency Key)가 필요합니다.");
        }

        // 2. 좌석 목록 검증
        if (seatIds == null || seatIds.isEmpty()) {
            log.warn("홀딩할 좌석 목록이 비어있음: scheduleId={}, userId={}", scheduleId, userId);
            throw new IllegalArgumentException("예약할 좌석이 없습니다.");
        }

        // 3. 이미 처리된 요청인지 확인
        Optional<HoldSeatResult> existingResult = holdSeatIdempotencyCachePort.findByIdempotencyKey(idempotencyKey);
        if (existingResult.isPresent()) {
            log.info("멱등성 키로 기존 결과 반환: idempotencyKey={}, result={}",
                    idempotencyKey, existingResult.get());
            return existingResult.get();
        }

        // 4. 좌석 ID 중복 제거
        List<Long> uniqueSeatIds = seatIds.stream()
                .distinct()
                .toList();

        // 5. 만약 현재 사용자가 모든 좌석을 홀딩하고 있다면 이미 예약하고 있으므로 결과 반환
        if (holdSeatCachePort.validateHoldSeatList(scheduleId, uniqueSeatIds, userId)) {
            long ttl = holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId);
            return HoldSeatResult.of(scheduleId, uniqueSeatIds, userId, ttl);
        }

        // 6. 좌석 예약 시도
        boolean isSuccess = holdSeatCachePort.holdSeatList(scheduleId, uniqueSeatIds, userId);
        if (!isSuccess)
            throw new IllegalStateException("다른 사용자가 이미 예약 중인 좌석입니다.");

        // 7. 결과 생성 및 멱등성 저장
        HoldSeatResult result = HoldSeatResult.of(
                scheduleId,
                uniqueSeatIds,
                userId,
                holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId)
        );

        try {
            holdSeatIdempotencyCachePort.saveResult(idempotencyKey, result);
            log.info("좌석 홀딩 성공 및 멱등성 저장: idempotencyKey={}, result={}",
                    idempotencyKey, result);
        } catch (Exception e) {
            // 멱등성 저장 실패는 비즈니스 로직 성공에 영향을 주지 않음
            log.error("멱등성 결과 저장 실패 (비즈니스 로직은 성공): idempotencyKey={}, result={}",
                    idempotencyKey, result, e);
        }
        return result;
    }
}
