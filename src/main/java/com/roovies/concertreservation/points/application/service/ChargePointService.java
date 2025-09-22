package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.dto.result.ChargePointResult;
import com.roovies.concertreservation.points.application.port.in.ChargePointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class ChargePointService implements ChargePointUseCase {

    private final PointRepositoryPort pointRepositoryPort;

    @Override
    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class, // 낙관적락 충돌 시 재시도
            maxAttempts = 5,                                         // 최대 5번 시도
            backoff = @Backoff(delay = 100, multiplier = 2.0)         // 100ms → 200ms → 400ms ...
    )
    public ChargePointResult execute(ChargePointCommand command) {
        Long userId = command.userId();
        Amount chargeAmount = Amount.of(command.amount());

        Point point = pointRepositoryPort.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));
        point.charge(chargeAmount);

        Point result = pointRepositoryPort.save(point);
        return ChargePointResult.builder()
                .userId(result.getUserId())
                .totalAmount(result.getAmount().value())
                .updatedAt(result.getUpdatedAt())
                .build();
    }
}
