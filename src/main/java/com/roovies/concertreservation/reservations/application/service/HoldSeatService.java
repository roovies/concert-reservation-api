package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.command.HoldSeatCommand;
import com.roovies.concertreservation.reservations.application.dto.result.HoldSeatResult;
import com.roovies.concertreservation.reservations.application.port.in.HoldSeatUseCase;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatCachePort;
import com.roovies.concertreservation.reservations.application.port.out.HoldSeatIdempotencyCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 좌석 홀딩(Hold Seat) 요청을 처리하는 서비스 구현체.
 * <p>
 * 멱등성 키(Idempotency Key)를 기반으로 중복 요청을 방지하고,
 * 캐시를 활용해 좌석 임시 선점을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HoldSeatService implements HoldSeatUseCase {

    private final HoldSeatCachePort holdSeatCachePort;
    private final HoldSeatIdempotencyCachePort holdSeatIdempotencyCachePort;

    /**
     * 좌석 홀딩 요청을 처리한다.
     *
     * @param command 좌석 홀딩 요청 정보 (멱등성 키, 스케줄 ID, 좌석 ID 목록, 사용자 ID 포함)
     * @return {@link HoldSeatResult} 홀딩된 좌석 및 TTL 정보
     * @throws IllegalArgumentException 멱등성 키가 없거나 좌석 목록이 비어있을 경우
     * @throws IllegalStateException 다른 사용자가 이미 좌석을 예약 중인 경우
     */
    @Override
    public HoldSeatResult holdSeat(HoldSeatCommand command) {
        String idempotencyKey = command.idempotencyKey();
        Long scheduleId = command.scheduleId();
        List<Long> seatIds = command.seatIds();
        Long userId = command.userId();

        log.info("[HoldSeatService] 좌석 홀딩 수행 - userId: {}, scheduleId: {}, seatIds: {}",
                userId, scheduleId, seatIds);

        // 멱등성 키 검증 및 키 선점
        HoldSeatResult existingResult = validateIdempotencyKeyAndGetResult(idempotencyKey);
        // 선점되어 있으면 결과 반환
        if (existingResult != null)
            return existingResult;

        try {
            // 좌석 ID 중복 제거
            List<Long> uniqueSeatIds = seatIds.stream()
                    .distinct()
                    .toList();

            // 이미 좌석을 홀딩하고 있는지 확인
            HoldSeatResult existingHoldResult = checkExistingHold(scheduleId, uniqueSeatIds, userId);
            if (existingHoldResult != null)
                return existingHoldResult;

            // 좌석 홀딩 수행
            boolean isSuccess = holdSeatCachePort.holdSeatList(scheduleId, uniqueSeatIds, userId);
            if (!isSuccess) {
                log.error("[HoldSeatService] 좌석 홀딩 실패(다른 사용자가 이미 예약 중) - userId: {}, scheduleId: {}, seatIds: {}",
                        userId, scheduleId, seatIds);
                throw new IllegalStateException("다른 사용자가 이미 예약 중인 좌석입니다.");
            }

            // 결과 생성 및 멱등성 저장
            HoldSeatResult result = HoldSeatResult.builder()
                    .scheduleId(scheduleId)
                    .seatIds(uniqueSeatIds)
                    .userId(userId)
                    .totalPrice(0L) // TODO: totalPrice 계산 후 적재해줘야 함
                    .ttlSeconds(holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId))
                    .build();

            holdSeatIdempotencyCachePort.saveResult(idempotencyKey, result);
            log.info("좌석 홀딩 성공 및 멱등성 저장완료 - idempotencyKey: {}, result: {}",
                    idempotencyKey, result);

            return result;
        } catch (Exception e) {
            // 결과가 저장되지 않은 경우에만 처리 상태 제거
            if (holdSeatIdempotencyCachePort.isProcessing(idempotencyKey))
                holdSeatIdempotencyCachePort.removeProcessingStatus(idempotencyKey);
            throw e;
        }
    }

    /**
     * 멱등성 키를 검증하고, 기존 결과가 있으면 반환한다.
     *
     * @param idempotencyKey 요청 식별자
     * @return 기존 결과가 있으면 {@link HoldSeatResult}, 없으면 {@code null}
     * @throws IllegalArgumentException 멱등성 키가 없거나, 동시성 문제가 발생한 경우
     */
    private HoldSeatResult validateIdempotencyKeyAndGetResult(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            throw new IllegalArgumentException("요청 식별자(Idempotency Key)가 필요합니다.");
        }

        if (!holdSeatIdempotencyCachePort.tryProcess(idempotencyKey)) {
            if (holdSeatIdempotencyCachePort.isProcessing(idempotencyKey)) {
                throw new IllegalArgumentException("중복된 요청입니다.");
            }

            HoldSeatResult existingResult = holdSeatIdempotencyCachePort.findByIdempotencyKey(idempotencyKey);
            if (existingResult != null) {
                log.info("멱등성 키로 기존 결과 반환 - idempotencyKey: {}, result: {}",
                        idempotencyKey, existingResult);
                return existingResult;
            } else {
                // 사실상 HTTP Request Timeout과 Redis TTL을 고려했을 때 멱등성 문제에서 해당 분기가 발생할 확률이 있을까..?
                throw new IllegalArgumentException("동시성 문제가 발생했습니다.");
            }
        }
        return null; // 새로운 요청으로 처리 계속
    }

    /**
     * 이미 사용자가 동일한 좌석을 홀딩 중인지 검증한다.
     *
     * @param scheduleId 스케줄 ID
     * @param uniqueSeatIds 중복 제거된 좌석 ID 목록
     * @param userId 사용자 ID
     * @return 기존 홀딩이 있으면 {@link HoldSeatResult}, 없으면 {@code null}
     * @throws IllegalArgumentException 좌석 목록이 비어있는 경우
     */
    private HoldSeatResult checkExistingHold(Long scheduleId, List<Long> uniqueSeatIds, Long userId) {
        // 3. 좌석 목록 검증
        if (uniqueSeatIds == null || uniqueSeatIds.isEmpty()) {
            log.warn("홀딩할 좌석 목록이 비어있음: scheduleId={}, userId={}", scheduleId, userId);
            throw new IllegalArgumentException("예약할 좌석이 없습니다.");
        }

        if (holdSeatCachePort.validateHoldSeatList(scheduleId, uniqueSeatIds, userId)) {
            long ttl = holdSeatCachePort.getHoldTTLSeconds(scheduleId, uniqueSeatIds, userId);
            // TODO: totalPrice 계산 후 적재해줘야 함
            return HoldSeatResult.builder()
                    .scheduleId(scheduleId)
                    .seatIds(uniqueSeatIds)
                    .userId(userId)
                    .totalPrice(0L)
                    .ttlSeconds(ttl)
                    .build();
        }
        return null;
    }
}
