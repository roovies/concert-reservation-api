package com.roovies.concertreservation.reservations.infra.adapter.out.persistence;

import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity.ReservationJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReservationRepositoryAdapter implements ReservationRepositoryPort {

    private final ReservationJpaRepository reservationJpaRepository;
    private final ReservationDetailJpaRepository reservationDetailJpaRepository;

    @Override
    public Optional<Reservation> findById(Long id) {
        Optional<ReservationJpaEntity> entity = reservationJpaRepository.findById(id);
        if (entity.isPresent()) {
            ReservationJpaEntity reservation = entity.get();
            return Optional.of(
                    Reservation.create(
                            reservation.getId(),
                            reservation.getUser().getId(),
                            reservation.getStatus(),
                            reservation.getCreatedAt(),
                            reservation.getUpdatedAt(),
                            new ArrayList<>()
                    )
            );
        }
        return Optional.empty();
    }

    @Override
    public List<Reservation> findReservationsByDetailScheduleId(Long scheduleId) {
        List<ReservationJpaEntity> entities = reservationJpaRepository.findByDetailScheduleIdWithDetails(scheduleId);

        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(entity -> {
                    List<ReservationDetail> details = entity.getReservationDetails().stream()
                            .map(detailEntity -> ReservationDetail.create(
                                    detailEntity.getId(),
                                    detailEntity.getReservation().getId(),
                                    detailEntity.getSchedule().getId(),
                                    detailEntity.getSeat().getId()
                            ))
                            .toList();

                    return Reservation.create(
                            entity.getId(),
                            entity.getUser().getId(),
                            entity.getStatus(),
                            entity.getCreatedAt(),
                            entity.getUpdatedAt(),
                            details
                    );
                })
                .toList();
    }
}
