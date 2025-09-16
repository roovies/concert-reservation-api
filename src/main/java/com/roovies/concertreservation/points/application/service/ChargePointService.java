package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.dto.command.ChargePointCommand;
import com.roovies.concertreservation.points.application.dto.result.ChargePointResult;
import com.roovies.concertreservation.points.application.port.in.ChargePointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.application.port.out.PointUserQueryPort;
import com.roovies.concertreservation.points.domain.vo.external.PointUserSnapShot;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ChargePointService implements ChargePointUseCase {

    private final PointRepositoryPort pointRepositoryPort;
    private final PointUserQueryPort pointUserQueryPort;

    @Override
    public ChargePointResult excute(ChargePointCommand command) {
        Long userId = command.userId();
        Amount amount = Amount.of(command.amount());

        PointUserSnapShot userSnapShot = pointUserQueryPort.getUser(userId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

        return null;
    }
}
