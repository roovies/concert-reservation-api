package com.roovies.concertreservation.venues.infra.adapter.out.persistence;

import com.roovies.concertreservation.venues.application.port.out.VenueQueryRepositoryPort;
import com.roovies.concertreservation.venues.domain.entity.Venue;
import com.roovies.concertreservation.venues.domain.entity.VenueSeat;
import com.roovies.concertreservation.venues.domain.vo.Money;
import com.roovies.concertreservation.venues.infra.adapter.out.persistence.entity.VenueJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class VenueQueryRepositoryAdapter implements VenueQueryRepositoryPort {
    private final VenueJpaRepository venueJpaRepository;
    private final VenueSeatJpaRepository venueSeatJpaRepository;

    @Override
    public Optional<Venue> findById(Long id) {
        Optional<VenueJpaEntity> entity = venueJpaRepository.findById(id);
        if (entity.isPresent()) {
            VenueJpaEntity venue = entity.get();
            return Optional.of(
                    Venue.create(
                            venue.getId(),
                            venue.getName(),
                            venue.getTotalSeats(),
                            venue.getCreatedAt(),
                            venue.getUpdatedAt()
                    )
            );
        }
        return Optional.empty();
    }

    @Override
    public Optional<Venue> findByIdWithSeats(@Param("id") Long venueId) {
        Optional<VenueJpaEntity> entity = venueJpaRepository.findByIdWithSeats(venueId);
        if (entity.isPresent()) {
            VenueJpaEntity venue = entity.get();

            List<VenueSeat> seats = venue.getSeats().stream()
                    .map(seatEntity -> VenueSeat.create(
                            seatEntity.getId(),
                            seatEntity.getRow(),
                            seatEntity.getSeatNumber(),
                            seatEntity.getType(),
                            new Money(seatEntity.getPrice()),
                            seatEntity.getCreatedAt()
                    ))
                    .toList();

            Venue domainVenue = Venue.create(
                    venue.getId(),
                    venue.getName(),
                    venue.getTotalSeats(),
                    venue.getCreatedAt(),
                    venue.getUpdatedAt()
            );
            domainVenue.setSeats(seats);
            return Optional.of(domainVenue);
        }
        return Optional.empty();
    }

    @Override
    public Long getTotalSeatsPrice(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return 0L;
        }

        return venueSeatJpaRepository.getTotalPriceBySeatIds(seatIds);
    }
}
