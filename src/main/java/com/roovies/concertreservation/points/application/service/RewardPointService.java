package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.dto.command.RewardPointCommand;
import com.roovies.concertreservation.points.application.dto.result.RewardPointResult;
import com.roovies.concertreservation.points.application.port.in.RewardPointUseCase;
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
 * 포인트 적립 서비스 구현체.
 * <p>
 * 결제 완료 후 사용자에게 리워드 포인트를 적립하는 기능을 제공하며,
 * 낙관적 락 충돌 발생 시 재시도 메커니즘을 적용한다.
 */
@Slf4j
@Service("rewardPointService")
@RequiredArgsConstructor
@Transactional
public class RewardPointService implements RewardPointUseCase {

    private final PointCommandRepositoryPort pointCommandRepositoryPort;

    /**
     * 사용자에게 리워드 포인트를 적립한다.
     * <p>
     * - 회원 정보를 조회 후, 요청된 금액만큼 포인트를 증가시킨다.<br>
     * - 적립 결과는 DB에 반영되며, 적립된 포인트 총액과 갱신 시간을 반환한다.<br>
     * - 낙관적 락 충돌(ObjectOptimisticLockingFailureException) 발생 시 최대 5회까지 재시도한다.
     *
     * @param command 포인트 적립 요청 객체
     * @return 적립 후 결과 객체
     * @throws NoSuchElementException 존재하지 않는 회원일 경우
     * @throws IllegalArgumentException 적립 금액이 100원 단위가 아닌 경우
     */
    @Override
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2.0)
    )
    public RewardPointResult reward(RewardPointCommand command) {
        Long userId = command.userId();
        Long rewardAmount = command.rewardAmount();
        Long paymentId = command.paymentId();

        log.info("[RewardPointService] 포인트 적립 시작 - userId: {}, rewardAmount: {}, paymentId: {}",
                userId, rewardAmount, paymentId);

        // 100원 단위 검증
        if (rewardAmount % 100 != 0) {
            throw new IllegalArgumentException("포인트는 100원 단위로만 적립할 수 있습니다.");
        }

        Point point = pointCommandRepositoryPort.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        // 포인트 적립 (charge 메서드는 100원 단위 검증도 수행)
        point.charge(Amount.of(rewardAmount));

        Point result = pointCommandRepositoryPort.save(point);

        log.info("[RewardPointService] 포인트 적립 성공 - userId: {}, rewardAmount: {}, totalAmount: {}, paymentId: {}",
                userId, rewardAmount, result.getAmount().value(), paymentId);

        return RewardPointResult.builder()
                .userId(result.getUserId())
                .rewardAmount(rewardAmount)
                .totalAmount(result.getAmount().value())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}