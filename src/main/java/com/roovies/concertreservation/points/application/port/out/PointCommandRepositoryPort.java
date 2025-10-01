package com.roovies.concertreservation.points.application.port.out;

import com.roovies.concertreservation.points.domain.entity.Point;

import java.util.Optional;

public interface PointCommandRepositoryPort {
    Optional<Point> findById(Long userId); // CUD 작업 시 원자적으로 사용할 조회 메서드
    Point save(Point point);
    void deleteAll();
}
