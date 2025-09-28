package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatListQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatListResult;
import com.roovies.concertreservation.reservations.application.port.in.GetAvailableSeatListUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueGatewayPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationConcertGatewayPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import com.roovies.concertreservation.reservations.domain.external.ExternalVenue;
import com.roovies.concertreservation.reservations.domain.external.ExternalConcertSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 예약 가능 좌석 조회 서비스 구현체.
 * <p>
 * 사용자가 특정 날짜의 콘서트 스케줄에 대해 예약 가능한 좌석 목록을 조회할 수 있도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetAvailableSeatListService implements GetAvailableSeatListUseCase {

    private final ReservationConcertGatewayPort reservationConcertGatewayPort;
    private final ReservationVenueGatewayPort reservationVenueGatewayPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    /**
     * 특정 콘서트와 날짜에 대한 예약 가능한 좌석 목록을 조회한다.
     * <p>
     * - 외부 게이트웨이를 통해 콘서트 스케줄을 조회한다.<br>
     * - 매진 상태라면 빈 좌석 목록과 함께 매진 결과를 반환한다.<br>
     * - 공연장 좌석과 예약 정보를 기반으로 예약 가능한 좌석 목록을 생성한다.
     *
     * @param query 예약 가능한 좌석 조회 요청 객체
     * @return 예약 가능 좌석 목록 결과
     */
    @Override
    public GetAvailableSeatListResult getAvailableSeatList(GetAvailableSeatListQuery query) {
        log.info("[GetAvailableSeatListService] 특정 날짜 예약 가능한 좌석 목록 조회 - concertId: {}, date: {}",
                query.concertId(), query.date());

        // 1. 해당 날짜의 콘서트 스케줄 정보 조회
        ExternalConcertSchedule schedule = reservationConcertGatewayPort.findConcertSchedule(query.concertId(), query.date());

        // 2. 예약 가능 좌석수, 예약 상태가 매진일 경우 매진 상태 반환
        if (schedule.isSoldOut())
            return GetAvailableSeatListResult.builder()
                    .concertId(query.concertId())
                    .concertScheduleId(schedule.id())
                    .date(query.date())
                    .isAllReserved(true)
                    .availableSeats(new ArrayList<>())
                    .build();

        // 3. 예약 가능한 좌석 조회
        List<GetAvailableSeatListResult.SeatItem> availableSeats = extractAvailableSeats(schedule);

        return GetAvailableSeatListResult.builder()
                .concertId(query.concertId())
                .concertScheduleId(schedule.id())
                .date(query.date())
                .isAllReserved(false)
                .availableSeats(availableSeats)
                .build();
    }

    /**
     * 공연장 좌석 및 예약 정보를 기반으로 예약 가능한 좌석을 추출한다.
     *
     * @param schedule 콘서트 스케줄
     * @return 예약 가능 좌석 목록
     */
    private List<GetAvailableSeatListResult.SeatItem> extractAvailableSeats(ExternalConcertSchedule schedule) {
        // 1. 공연장 및 좌석 조회
        ExternalVenue venue = reservationVenueGatewayPort.findVenueWithSeats(schedule.venueId());

        // 2. 해당 스케줄에 등록된 예약 목록 조회
        List<Reservation> reservations = reservationRepositoryPort.findReservationsByDetailScheduleId(schedule.id());

        // 3. 예약된 좌석 ID 추출
        Set<Long> reservedSeatIds = reservations.stream()
                .filter(reservation -> reservation.getStatus() == ReservationStatus.HOLD
                        || reservation.getStatus() == ReservationStatus.CONFIRMED)
                .flatMap(reservation -> reservation.getDetails().stream())
                .filter(detail -> detail.getScheduleId().equals(schedule.id()))
                .map(ReservationDetail::getSeatId)
                .collect(Collectors.toSet());

        // 4. 예약가능한 좌석만 추출
        return venue.seats().stream()
                .filter(seat -> !reservedSeatIds.contains(seat.id()))
                .map(seat -> GetAvailableSeatListResult.SeatItem.builder()
                        .seatId(seat.id())
                        .row(seat.row())
                        .seatNumber(seat.seatNumber())
                        .seatType(seat.seatType())
                        .price(seat.price())
                        .build()
                )
                .toList();
    }
}
