package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerthalls.application.port.out.ConcertHallRepositoryPort;
import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;
import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertHallRepositoryAdapter implements ConcertHallRepositoryPort {
    private final ConcertHallJpaRepository concertHallJpaRepository;

    @Override
    public Optional<ConcertHall> findById(Long id) {
        Optional<ConcertHallJpaEntity> entity = concertHallJpaRepository.findById(id);
        if (entity.isEmpty()) {
            ConcertHallJpaEntity concertHall = new ConcertHallJpaEntity();
            return Optional.of(
                    ConcertHall.create(
                            concertHall.getId(),
                            concertHall.getName(),
                            concertHall.getTotalSeats(),
                            concertHall.getCreatedAt(),
                            concertHall.getUpdatedAt()
                    )
            );
        }
        return Optional.empty();
    }
}
