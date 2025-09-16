package com.roovies.concertreservation.points.infra.adapter.out.persistence;

import com.roovies.concertreservation.points.application.port.out.PointRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.points.infra.adapter.out.persistence.entity.PointJpaEntity;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointRepositoryAdapter implements PointRepositoryPort {

    private final PointJpaRepository pointJpaRepository;

    @Override
    public Optional<Point> findById(Long userId) {
        Optional<PointJpaEntity> entity = pointJpaRepository.findById(userId);
        if (entity.isPresent()) {
            PointJpaEntity point = entity.get();
            return Optional.of(
                    Point.create(
                            point.getUserId(),
                            Amount.of(point.getAmount()),
                            point.getUpdatedAt()
                    )
            );
        }
        return Optional.empty();
    }

    @Override
    public Point save(Point point) {
        PointJpaEntity entity = PointJpaEntity.from(point);
        PointJpaEntity result = pointJpaRepository.save(entity);
        return Point.create(
                result.getUserId(),
                Amount.of(result.getAmount()),
                result.getUpdatedAt()
        );
    }
}
