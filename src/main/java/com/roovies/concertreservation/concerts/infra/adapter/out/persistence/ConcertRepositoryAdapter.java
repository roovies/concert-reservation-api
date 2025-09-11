package com.roovies.concertreservation.concerts.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerts.application.port.out.ConcertRepositoryPort;
import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertJpaEntity;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertScheduleJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertRepositoryAdapter implements ConcertRepositoryPort {

    private final ConcertJpaRepository concertJpaRepository;
    private final ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Override
    public Optional<Concert> findById(Long id) {
        Optional<ConcertJpaEntity> entity = concertJpaRepository.findById(id);
        if (entity.isPresent()) {
            ConcertJpaEntity concert = entity.get();
            return Optional.of(
                    Concert.create(
                            concert.getId(),
                            concert.getTitle(),
                            concert.getDescription(),
                            concert.getMinPrice(),
                            concert.getStartDate(),
                            concert.getEndDate(),
                            concert.getCreatedAt(),
                            concert.getUpdatedAt()
                    )
            );
        }
        return Optional.empty();
    }

    @Override
    public Optional<Concert> findByIdWithSchedules(Long id) {
        Optional<ConcertJpaEntity> entity = concertJpaRepository.findById(id);
        if (entity.isPresent()) {
            ConcertJpaEntity concert = entity.get();
            List<ConcertScheduleJpaEntity> schedules = concertScheduleJpaRepository.findByConcertId(concert.getId());
            concert.setSchedules(schedules);
        }
        return Optional.empty();
    }
}
