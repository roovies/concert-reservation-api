package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.dto.command.DeductPointCommand;
import com.roovies.concertreservation.points.application.dto.result.DeductPointResult;
import com.roovies.concertreservation.points.application.port.in.DeductPointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointCommandRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 포인트 차감 서비스 구현체.
 * <p>
 * 사용자의 포인트를 차감하는 기능을 제공하며,
 * 낙관적 락 충돌 발생 시 재시도 메커니즘을 적용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeductPointService implements DeductPointUseCase {

    private final PointCommandRepositoryPort pointCommandRepositoryPort;

    /**
     * 사용자의 포인트를 차감한다.
     * <p>
     * - 회원 정보를 조회 후, 요청된 금액만큼 포인트를 감소시킨다.<br>
     * - 차감 결과는 DB에 반영되며, 차감 후 남은 포인트 총액과 갱신 시간을 반환한다.<br>
     * - 낙관적 락 충돌(ObjectOptimisticLockingFailureException) 발생 시 최대 5회까지 재시도한다.
     *
     * @param command 포인트 차감 요청 객체
     * @return 차감 후 결과 객체
     * @throws NoSuchElementException 존재하지 않는 회원일 경우
     */
    @Override
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class, // 낙관적락 충돌 시 재시도
            maxAttempts = 5,                                         // 최대 5번 시도
            backoff = @Backoff(delay = 100, multiplier = 2.0)         // 100ms → 200ms → 400ms ...
    )
    public DeductPointResult deduct(DeductPointCommand command) {
        Long userId = command.userId();
        Amount deductAmount = Amount.of(command.amount());

        log.info("[DeductPointService] 포인트 차감 시작 - userId: {}, deductAmount: {}",
                userId, deductAmount);

        Point point = pointCommandRepositoryPort.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        // 포인트 차감
        point.deduct(deductAmount);
        Point result = pointCommandRepositoryPort.save(point);

        log.info("[DeductPointService] 포인트 차감 성공 - userId: {}, deductAmount: {}, totalAmount: {}",
                userId, deductAmount, result.getAmount());

        return DeductPointResult.builder()
                .userId(result.getUserId())
                .resultAmount(result.getAmount().value())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
