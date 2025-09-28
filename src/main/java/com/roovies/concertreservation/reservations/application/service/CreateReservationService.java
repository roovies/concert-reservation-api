package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.command.CreateReservationCommand;
import com.roovies.concertreservation.reservations.application.port.in.CreateReservationUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 생성 서비스 구현체.
 * <p>
 * 사용자가 결제를 완료한 후 예약 및 예약 상세 정보를 생성하고 저장하는 기능을 제공한다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CreateReservationService implements CreateReservationUseCase {

    private final ReservationRepositoryPort reservationRepositoryPort;

    /**
     * 예약 및 예약 상세 정보를 생성한다.
     * <p>
     * - 좌석 ID 목록을 기반으로 {@link ReservationDetail} 엔티티를 생성한다.<br>
     * - 예약 엔티티를 생성하고 현재 시간을 예약 시각으로 설정한다.<br>
     * - 생성된 예약을 저장소에 저장한다.
     *
     * @param command 예약 생성 요청 객체
     */
    @Override
    public void createReservation(CreateReservationCommand command) {
        Long paymentId = command.paymentId();
        Long userId = command.userId();
        ReservationStatus status = command.status();
        Long scheduleId = command.scheduleId();
        List<Long> seatIds = command.seatIds();

        log.info("[CreateReservationService] 예약 및 예약 상세 생성 시작 - paymentId: {}, userId: {}, scheduleId: {}, seatIds: {}",
                paymentId, userId, scheduleId, seatIds);

        // 예약 및 예약 상세 정보 Domain Entity 생성
        List<ReservationDetail> details = seatIds.stream()
                .map(seatId -> ReservationDetail.create(
                        null,
                        scheduleId,
                        seatId,
                        userId
                ))
                .toList();

        Reservation reservation = Reservation.create(
                null,
                userId,
                paymentId,
                status,
                LocalDateTime.now(),
                null,
                details
        );

        // 저장
        reservationRepositoryPort.save(reservation);

        log.info("[CreateReservationService] 예약 및 예약 상세 생성 성공 - paymentId: {}, userId: {}, scheduleId: {}, seatIds: {}",
                paymentId, userId, scheduleId, seatIds);
    }
}
