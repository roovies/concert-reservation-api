package com.roovies.concertreservation.reservations.infra.adapter.out.persistence;

import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.infra.adapter.out.persistence.entity.ReservationDetailJpaEntity;
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
    public Optional<Reservation> findById(Long reservationId) {
        Optional<ReservationJpaEntity> entity = reservationJpaRepository.findById(reservationId);
        if (entity.isPresent()) {
            ReservationJpaEntity reservation = entity.get();
            return Optional.of(
                    Reservation.create(
                            reservation.getId(),
                            reservation.getUserId(),
                            reservation.getPaymentId(),
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
                                    detailEntity.getScheduleId(),
                                    detailEntity.getSeatId()
                            ))
                            .toList();

                    return Reservation.create(
                            entity.getId(),
                            entity.getUserId(),
                            entity.getPaymentId(),
                            entity.getStatus(),
                            entity.getCreatedAt(),
                            entity.getUpdatedAt(),
                            details
                    );
                })
                .toList();
    }

    @Override
    public void save(Reservation reservation) {
        List<ReservationDetailJpaEntity> details = new ArrayList<>();

        // Reservation 설정 (detail은 참조값만 할당)
        ReservationJpaEntity reservationJpaEntity = ReservationJpaEntity.builder()
                .paymentId(reservation.getPaymentId())
                .userId(reservation.getUserId())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .reservationDetails(details)
                .build();

        // Reservation Detail 설정 (참조값 객체 적재)
        for (ReservationDetail detail : reservation.getDetails()) {
            ReservationDetailJpaEntity detailJpaEntity = ReservationDetailJpaEntity.builder()
                    .reservation(reservationJpaEntity)
                    .scheduleId(detail.getScheduleId())
                    .seatId(detail.getSeatId())
                    .build();

            details.add(detailJpaEntity);
        }

        // 일괄 저장 (cascade로 인하여 details도 함께 저장)
        reservationJpaRepository.save(reservationJpaEntity);
    }
}
