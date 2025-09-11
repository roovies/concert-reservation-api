package com.roovies.concertreservation.reservations.application.service;

import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatsResult;
import com.roovies.concertreservation.reservations.application.port.in.GetAvailableSeatsUseCase;
import com.roovies.concertreservation.reservations.application.port.out.ConcertHallQueryPort;
import com.roovies.concertreservation.reservations.application.port.out.ConcertQueryPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.enums.PaymentStatus;
import com.roovies.concertreservation.reservations.domain.vo.external.ConcertHallSnapShot;
import com.roovies.concertreservation.reservations.domain.vo.external.ConcertScheduleSnapShot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetAvailableSeatsService implements GetAvailableSeatsUseCase {

    private final ConcertQueryPort concertQueryPort;
    private final ConcertHallQueryPort concertHallQueryPort;
    private final ReservationRepositoryPort reservationRepositoryPort;

    @Override
    public GetAvailableSeatsResult execute(GetAvailableSeatsQuery query) {
        // 1. 해당 날짜의 콘서트 스케줄 정보 조회
        ConcertScheduleSnapShot schedule = concertQueryPort.findConcertSchedule(query.concertId(), query.date());

        // 2. 예약 가능 좌석수, 예약 상태가 매진일 경우 매진 상태 반환
        if (schedule.isSoldOut())
            return GetAvailableSeatsResult.builder()
                    .concertId(query.concertId())
                    .date(query.date())
                    .isAllReserved(true)
                    .availableSeats(new ArrayList<>())
                    .build();

        // 3. 예약이 가능할 경우 공연장 및 공연장 좌석 정보 조회
        ConcertHallSnapShot concertHall = concertHallQueryPort.findConcertHallById(schedule.concertHallId());

        // 4. 해당 스케줄에 등록된 예약 목록 조회
        List<Reservation> reservations = reservationRepositoryPort.findReservationsByDetailScheduleId(schedule.id());

        // 5. 예약된 좌석ID 추출
        Set<Long> reservedSeatIds = reservations.stream()
                .filter(reservation -> reservation.getStatus().equals(PaymentStatus.HOLD)
                        || reservation.getStatus().equals(PaymentStatus.CONFIRMED))
                .flatMap(reservation -> reservation.getDetails().stream())
                .filter(detail -> detail.getScheduleId().equals(schedule.id()))
                .map(detail -> detail.getSeatId())
                .collect(Collectors.toSet());

        // 6. 예약가능한 좌석들만 추출
        List<GetAvailableSeatsResult.SeatInfo> availableSeats = concertHall.seats().stream()
                .filter(seat -> !reservedSeatIds.contains(seat.id()))
                .map(seat -> GetAvailableSeatsResult.SeatInfo.builder()
                        .seatId(seat.id())
                        .row(seat.row())
                        .seatNumber(seat.seatNumber())
                        .seatType(seat.seatType())
                        .price(seat.price())
                        .build()
                )
                .toList();

        return GetAvailableSeatsResult.builder()
                .concertId(query.concertId())
                .date(query.date())
                .isAllReserved(false)
                .availableSeats(availableSeats)
                .build();
    }
}
