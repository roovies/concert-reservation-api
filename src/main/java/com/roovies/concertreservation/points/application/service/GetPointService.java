package com.roovies.concertreservation.points.application.service;

import com.roovies.concertreservation.points.application.port.in.GetPointUseCase;
import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * 포인트 조회 서비스 구현체.
 * <p>
 * 사용자의 포인트 정보를 조회하는 기능을 제공한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPointService implements GetPointUseCase {

    private final PointRepositoryPort pointRepositoryPort;

    /**
     * 특정 사용자의 포인트를 조회한다.
     * <p>
     * - 사용자 ID로 포인트 정보를 조회한다.<br>
     * - 포인트 정보가 존재하지 않을 경우 예외를 발생시킨다.<br>
     * - 존재할 경우 현재 포인트 금액을 반환한다.
     *
     * @param userId 사용자 ID
     * @return 사용자의 보유 포인트 금액
     * @throws NoSuchElementException 포인트 정보가 존재하지 않는 경우
     */
    @Override
    public Long findById(Long userId) {
        log.info("[GetPointService] 포인트 정보 조회 - userId: {}", userId);

        Point point = pointRepositoryPort.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("포인트 정보가 존재하지 않습니다."));
        return point.getAmount().value();
    }
}
