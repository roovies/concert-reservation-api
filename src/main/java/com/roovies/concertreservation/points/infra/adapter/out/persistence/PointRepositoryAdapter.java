package com.roovies.concertreservation.points.infra.adapter.out.persistence;

import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointRepositoryAdapter implements PointRepositoryPort {
    @Override
    public Optional<Point> findById(Long userId) {
        return Optional.empty();
    }

    @Override
    public void save(Point point) {

    }
}
