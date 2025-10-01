package com.roovies.concertreservation.concerts.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerts.application.port.out.ConcertQueryRepositoryPort;
import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.domain.entity.ConcertSchedule;
import com.roovies.concertreservation.concerts.infra.adapter.out.persistence.entity.ConcertJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertQueryRepositoryAdapter implements ConcertQueryRepositoryPort {

    private final ConcertJpaRepository concertJpaRepository;
    private final ConcertScheduleJpaRepository concertScheduleJpaRepository;

    @Override
    public Optional<Concert> findById(Long concertId) {
        Optional<ConcertJpaEntity> entity = concertJpaRepository.findById(concertId);
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
    // TODO: CQRS 패턴을 적용하여 Read Model 만들어서 서로 다른 BC간의 데이터 조회하기
    public Optional<Concert> findByIdWithSchedules(Long concertId) {
        Optional<ConcertJpaEntity> entity = concertJpaRepository.findByIdWithSchedules(concertId);
        if (entity.isPresent()) {
            ConcertJpaEntity concert = entity.get();

            List<ConcertSchedule> schedules = concert.getSchedules().stream()
                    .map(scheduleEntity -> ConcertSchedule.create(
                            scheduleEntity.getId(),
                            scheduleEntity.getConcert().getId(),
                            scheduleEntity.getScheduleDate(),
                            scheduleEntity.getAvailableSeats(), // 총 좌석수는 임시로 설정 -> Application에서 orchestration을 통해 적재
                            scheduleEntity.getAvailableSeats(),
                            scheduleEntity.getScheduleStatus(),
                            scheduleEntity.getVenueId()
                    ))
                    .toList();

            Concert domainConcert = Concert.create(
                    concert.getId(),
                    concert.getTitle(),
                    concert.getDescription(),
                    concert.getMinPrice(),
                    concert.getStartDate(),
                    concert.getEndDate(),
                    concert.getCreatedAt(),
                    concert.getUpdatedAt()
            );
            domainConcert.setSchedules(schedules);
            return Optional.of(domainConcert);
        }
        return Optional.empty();
    }
}
