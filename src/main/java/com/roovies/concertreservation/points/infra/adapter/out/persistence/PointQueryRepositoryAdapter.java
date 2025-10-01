package com.roovies.concertreservation.points.infra.adapter.out.persistence;

import com.roovies.concertreservation.points.application.port.out.PointQueryRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.points.infra.adapter.out.persistence.entity.PointJpaEntity;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointQueryRepositoryAdapter implements PointQueryRepositoryPort {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findById(Long userId) {
        Optional<PointJpaEntity> entity = pointJpaRepository.findById(userId);
        if (entity.isPresent()) {
            PointJpaEntity point = entity.get();
            return Optional.of(
                    Point.createWithVersion(
                            point.getUserId(),
                            Amount.of(point.getAmount()),
                            point.getUpdatedAt(),
                            point.getVersion()
                    )
            );
        }
        return Optional.empty();
    }
}
