package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.dto.result.ChargePointResult;
import com.roovies.concertreservation.points.application.port.in.ChargePointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ChargePointService implements ChargePointUseCase {

    private final PointRepositoryPort pointRepositoryPort;

    @Override
    public ChargePointResult excute(ChargePointCommand command) {
        Long userId = command.userId();
        Amount chargeAmount = Amount.of(command.amount());

        Point point = pointRepositoryPort.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        point.charge(chargeAmount);

        Point result = pointRepositoryPort.save(point);
        return ChargePointResult.of(
                result.getUserId(),
                result.getAmount().value(),
                result.getUpdatedAt()
        );
    }
}
