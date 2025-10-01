package com.roovies.concertreservation.points.infra.adapter.out.persistence;

import com.roovies.concertreservation.points.application.port.out.PointCommandRepositoryPort;
import com.roovies.concertreservation.points.domain.entity.Point;
import com.roovies.concertreservation.points.infra.adapter.out.persistence.entity.PointJpaEntity;
import com.roovies.concertreservation.shared.domain.vo.Amount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.NoSuchElementException;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PointCommandRepositoryAdapter implements PointCommandRepositoryPort {

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

    @Override
    // Domain Entity를 기반으로 새 JPA Entity를 만들어서(from) save할 경우 version 추적이 안됨
    // 따라서 version 정보가 있을 경우 비효율적일 수 있지만 다시 findBy()로 조회한 후 수정하는 방식으로 변경
    public Point save(Point point) {
        if (point.getVersion() == null) {
            // 새로운 엔티티
            PointJpaEntity entity = PointJpaEntity.from(point);
            PointJpaEntity result = pointJpaRepository.save(entity);
            return Point.createWithVersion(
                    result.getUserId(),
                    Amount.of(result.getAmount()),
                    result.getUpdatedAt(),
                    result.getVersion()
            );
        } else {
            // 기존 엔티티 업데이트
            PointJpaEntity entity = pointJpaRepository.findById(point.getUserId())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 회원입니다."));

            // Domain의 계산 결과를 JPA Entity에 적용
            // 도메인 엔터티에서 이미 수행했으므로 해당 메서드에서 += 수행하지 않아도 됨
            entity.updateAmount(point.getAmount().value(), point.getUpdatedAt());
            PointJpaEntity result = pointJpaRepository.save(entity);
            return Point.createWithVersion(
                    result.getUserId(),
                    Amount.of(result.getAmount()),
                    result.getUpdatedAt(),
                    result.getVersion()
            );
        }
    }

    @Override
    public void deleteAll() {
        pointJpaRepository.deleteAll();
    }
}
