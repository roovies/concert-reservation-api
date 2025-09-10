package com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence;

import com.roovies.concertreservation.concerthalls.application.port.out.ConcertHallRepositoryPort;
import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;
import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHallSeat;
import com.roovies.concertreservation.concerthalls.domain.vo.Money;
import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallJpaEntity;
import com.roovies.concertreservation.concerthalls.infra.adapter.out.persistence.entity.ConcertHallSeatJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ConcertHallRepositoryAdapter implements ConcertHallRepositoryPort {
    private final ConcertHallJpaRepository concertHallJpaRepository;
    private final ConcertHallSeatJpaRepository concertHallSeatJpaRepository;

    @Override
    public Optional<ConcertHall> findById(Long id) {
        Optional<ConcertHallJpaEntity> entity = concertHallJpaRepository.findById(id);
        if (entity.isPresent()) {
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

    @Override
    public Optional<ConcertHall> findByIdWithSeats(Long concertHallId) {
        Optional<ConcertHall> concertHall = findById(concertHallId);
        if (concertHall.isPresent()) {
            List<ConcertHallSeatJpaEntity> seatEntities = concertHallSeatJpaRepository.findByConcertHallId(concertHallId);
            List<ConcertHallSeat> seats = seatEntities.stream()
                    .map(entity -> ConcertHallSeat.create(
                            entity.getId(),
                            entity.getRow(),
                            entity.getSeatNumber(),
                            entity.getType(),
                            new Money(entity.getPrice()),
                            entity.getCreatedAt()
                    ))
                    .toList();

            concertHall.get().setSeats(seats);
            return concertHall;
        }
        return Optional.empty();
    }
}
