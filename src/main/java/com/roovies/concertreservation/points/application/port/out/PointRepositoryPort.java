package com.roovies.concertreservation.points.application.port.out;

import com.roovies.concertreservation.points.domain.entity.Point;

import java.util.Optional;

public interface PointRepositoryPort {
    Optional<Point> findById(Long userId);
    void save(Point point);
}
