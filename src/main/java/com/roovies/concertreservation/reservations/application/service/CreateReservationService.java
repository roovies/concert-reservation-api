package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.command.CreateReservationCommand;
import com.roovies.concertreservation.reservations.application.port.in.CreateReservationUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateReservationService implements CreateReservationUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;

    @Override
    public void createReservation(CreateReservationCommand command) {
        // 예약 및 예약 상세 정보 Domain Entity 생성
        List<ReservationDetail> details = command.seatIds().stream()
                .map(seatId -> ReservationDetail.create(
                        null,
                        command.scheduleId(),
                        seatId,
                        command.userId()
                ))
                .toList();

        Reservation reservation = Reservation.create(
                null,
                command.userId(),
                command.paymentId(),
                command.status(),
                LocalDateTime.now(),
                null,
                details
        );

        // 저장
        reservationRepositoryPort.save(reservation);
    }
}
