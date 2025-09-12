package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatListResult;
import com.roovies.concertreservation.reservations.application.port.in.GetAvailableSeatsUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueQueryPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationConcertQueryPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.domain.enums.PaymentStatus;
import com.roovies.concertreservation.reservations.domain.vo.external.ReservationVenueSnapShot;
import com.roovies.concertreservation.reservations.domain.vo.external.ReservationConcertScheduleSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAvailableSeatListService implements GetAvailableSeatsUseCase {

    private final ReservationConcertQueryPort reservationConcertQueryPort;
    private final ReservationVenueQueryPort reservationVenueQueryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    @Override
    public GetAvailableSeatListResult execute(GetAvailableSeatsQuery query) {
        // 1. 해당 날짜의 콘서트 스케줄 정보 조회
        ReservationConcertScheduleSnapShot schedule = reservationConcertQueryPort.findConcertSchedule(query.concertId(), query.date());

        // 2. 예약 가능 좌석수, 예약 상태가 매진일 경우 매진 상태 반환
        if (schedule.isSoldOut())
            return GetAvailableSeatListResult.builder()
                    .concertId(query.concertId())
                    .date(query.date())
                    .isAllReserved(true)
                    .availableSeats(new ArrayList<>())
                    .build();

        // 3. 예약 가능한 좌석 조회
        List<GetAvailableSeatListResult.SeatInfo> availableSeats = getAvailableSeats(schedule);

        return GetAvailableSeatListResult.builder()
                .concertId(query.concertId())
                .date(query.date())
                .isAllReserved(false)
                .availableSeats(availableSeats)
                .build();
    }

    private List<GetAvailableSeatListResult.SeatInfo> getAvailableSeats(ReservationConcertScheduleSnapShot schedule) {
        // 1. 공연장 및 좌석 조회
        ReservationVenueSnapShot venue = reservationVenueQueryPort.findVenueWithSeats(schedule.venueId());

        // 2. 해당 스케줄에 등록된 예약 목록 조회
        List<Reservation> reservations = reservationRepositoryPort.findReservationsByDetailScheduleId(schedule.id());

        // 3. 예약된 좌석 ID 추출
        Set<Long> reservedSeatIds = reservations.stream()
                .filter(reservation -> reservation.getStatus() == PaymentStatus.HOLD
                        || reservation.getStatus() == PaymentStatus.CONFIRMED)
                .flatMap(reservation -> reservation.getDetails().stream())
                .filter(detail -> detail.getScheduleId().equals(schedule.id()))
                .map(ReservationDetail::getSeatId)
                .collect(Collectors.toSet());

        // 4. 예약가능한 좌석만 추출
        return venue.seats().stream()
                .filter(seat -> !reservedSeatIds.contains(seat.id()))
                .map(seat -> GetAvailableSeatListResult.SeatInfo.builder()
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
