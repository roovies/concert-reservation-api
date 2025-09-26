package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.port.in.GetPointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class GetPointService implements GetPointUseCase {

    private final PointRepositoryPort pointRepositoryPort;

    @Override
    public Long findById(Long userId) {
        Point point = pointRepositoryPort.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("포인트 정보가 존재하지 않습니다."));

        return point.getAmount().value();
    }
}
