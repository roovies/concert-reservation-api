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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAvailableSeatListService implements GetAvailableSeatListUseCase {

    private final ReservationConcertGatewayPort reservationConcertGatewayPort;
    private final ReservationVenueGatewayPort reservationVenueGatewayPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    @Override
    public GetAvailableSeatListResult getAvailableSeatList(GetAvailableSeatListQuery query) {
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
        List<GetAvailableSeatListResult.SeatItem> availableSeats = getAvailableSeats(schedule);

        return GetAvailableSeatListResult.builder()
                .concertId(query.concertId())
                .concertScheduleId(schedule.id())
                .date(query.date())
                .isAllReserved(false)
                .availableSeats(availableSeats)
                .build();
    }

    private List<GetAvailableSeatListResult.SeatItem> getAvailableSeats(ExternalConcertSchedule schedule) {
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
