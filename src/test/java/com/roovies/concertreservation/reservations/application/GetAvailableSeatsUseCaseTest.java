package com.roovies.concertreservation.reservations.application;

import com.roovies.concertreservation.concerts.domain.enums.ScheduleStatus;
import com.roovies.concertreservation.venues.domain.enums.SeatType;
import com.roovies.concertreservation.reservations.application.dto.query.GetAvailableSeatsQuery;
import com.roovies.concertreservation.reservations.application.dto.result.GetAvailableSeatListResult;
import com.roovies.concertreservation.reservations.application.port.out.ReservationVenueQueryPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationConcertQueryPort;
import com.roovies.concertreservation.reservations.application.port.out.ReservationRepositoryPort;
import com.roovies.concertreservation.reservations.application.service.GetAvailableSeatListService;
import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import com.roovies.concertreservation.reservations.domain.vo.external.ReservationVenueSnapShot;
import com.roovies.concertreservation.reservations.domain.vo.external.ReservationConcertScheduleSnapShot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class GetAvailableSeatsUseCaseTest {

    @Mock
    private ReservationRepositoryPort reservationRepositoryPort;

    @Mock
    private ReservationConcertQueryPort reservationConcertQueryPort;

    @Mock
    private ReservationVenueQueryPort reservationVenueQueryPort;

    @InjectMocks
    private GetAvailableSeatListService getAvailableSeatListService;

    @Test
    void 콘서트나_일정이_유효하지_않으면_예외가_발생해야_한다() {
        // given
        GetAvailableSeatsQuery query = GetAvailableSeatsQuery.builder()
                .concertId(1L)
                .date(LocalDate.of(2025, 9, 10))
                .build();

        given(reservationConcertQueryPort.findConcertSchedule(query.concertId(), query.date()))
                .willThrow(NoSuchElementException.class);

        // when & then
        assertThatThrownBy(() -> getAvailableSeatListService.execute(query))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void 조회한_콘서트_일정의_잔여좌석이_0일_경우_좌석정보없이_매진_상태로_응답해야_한다() {
        // given
        LocalDate date = LocalDate.of(2025, 9, 10);
        GetAvailableSeatsQuery query = GetAvailableSeatsQuery.builder()
                .concertId(1L)
                .date(date)
                .build();

        ReservationConcertScheduleSnapShot schedule = ReservationConcertScheduleSnapShot.builder()
                .id(5L)
                .date(date)
                .availableSeats(0)
                .status(ScheduleStatus.AVAILABLE)
                .venueId(10L)
                .build();
        given(reservationConcertQueryPort.findConcertSchedule(query.concertId(), query.date()))
                .willReturn(schedule);

        // when
        GetAvailableSeatListResult result = getAvailableSeatListService.execute(query);

        // then
        assertThat(result).isNotNull();
        assertThat(result.concertId()).isEqualTo(query.concertId());
        assertThat(result.concertScheduleId()).isEqualTo(schedule.id());
        assertThat(result.date()).isEqualTo(query.date());
        assertThat(result.availableSeats()).isEmpty();
        assertThat(result.isAllReserved()).isTrue();
    }

    @Test
    // 잔여 좌석이 남았더라도, 주최측 사정으로 매진시킬 수도 있는 상황을 고려하였음 (코로나 거리두기로 좌석 띄어앉기 등)
    void 조회한_콘서트_일정의_예약상태가_SOLDOUT일_경우_좌석정보없이_매진_상태로_응답해야_한다() {
        // given
        LocalDate date = LocalDate.of(2025, 9, 10);
        GetAvailableSeatsQuery query = GetAvailableSeatsQuery.builder()
                .concertId(1L)
                .date(date)
                .build();

        ReservationConcertScheduleSnapShot schedule = ReservationConcertScheduleSnapShot.builder()
                .id(5L)
                .date(date)
                .availableSeats(50)
                .status(ScheduleStatus.SOLD_OUT)
                .venueId(10L)
                .build();
        given(reservationConcertQueryPort.findConcertSchedule(query.concertId(), query.date()))
                .willReturn(schedule);

        // when
        GetAvailableSeatListResult result = getAvailableSeatListService.execute(query);

        // then
        assertThat(result).isNotNull();
        assertThat(result.concertId()).isEqualTo(query.concertId());
        assertThat(result.concertScheduleId()).isEqualTo(schedule.id());
        assertThat(result.date()).isEqualTo(query.date());
        assertThat(result.availableSeats()).isEmpty();
        assertThat(result.isAllReserved()).isTrue();
    }

    @Test
    void 예약된_좌석이_없을_경우_모든_좌석이_예약_가능해야_한다() {
        // given
        LocalDate date = LocalDate.of(2025, 9, 10);
        GetAvailableSeatsQuery query = GetAvailableSeatsQuery.builder()
                .concertId(1L)
                .date(date)
                .build();

        ReservationConcertScheduleSnapShot schedule = ReservationConcertScheduleSnapShot.builder()
                .id(5L)
                .date(date)
                .availableSeats(3)
                .status(ScheduleStatus.AVAILABLE)
                .venueId(10L)
                .build();
        given(reservationConcertQueryPort.findConcertSchedule(query.concertId(), query.date()))
                .willReturn(schedule);

        ReservationVenueSnapShot venue = ReservationVenueSnapShot.builder()
                .id(10L)
                .name("인천 아시아드 주경기장")
                .totalSeats(3)
                .seats(List.of(
                        ReservationVenueSnapShot.VenueSeatInfo.builder()
                                .id(1L)
                                .row(1)
                                .seatNumber(1)
                                .seatType(SeatType.STANDARD)
                                .build(),
                        ReservationVenueSnapShot.VenueSeatInfo.builder()
                                .id(1L)
                                .row(1)
                                .seatNumber(2)
                                .seatType(SeatType.STANDARD)
                                .build(),
                        ReservationVenueSnapShot.VenueSeatInfo.builder()
                                .id(1L)
                                .row(1)
                                .seatNumber(3)
                                .seatType(SeatType.STANDARD)
                                .build()
                ))
                .build();
        given(reservationVenueQueryPort.findVenueWithSeats(schedule.venueId()))
                .willReturn(venue);

        given(reservationRepositoryPort.findReservationsByDetailScheduleId(schedule.id()))
                .willReturn(Collections.emptyList());

        // when
        GetAvailableSeatListResult result = getAvailableSeatListService.execute(query);

        // then
        assertThat(result).isNotNull();
        assertThat(result.concertId()).isEqualTo(query.concertId());
        assertThat(result.concertScheduleId()).isEqualTo(schedule.id());
        assertThat(result.date()).isEqualTo(query.date());
        assertThat(result.availableSeats()).hasSize(3);
        assertThat(result.isAllReserved()).isFalse();
    }


    @Test
    void 일부_좌석이_예약된_경우_예약되지_않은_좌석만_조회되어야_한다() {
        // given
        LocalDate date = LocalDate.of(2025, 9, 10);
        GetAvailableSeatsQuery query = GetAvailableSeatsQuery.builder()
                .concertId(1L)
                .date(date)
                .build();
        ReservationConcertScheduleSnapShot schedule = ReservationConcertScheduleSnapShot.builder()
                .id(5L)
                .date(date)
                .availableSeats(3)
                .status(ScheduleStatus.AVAILABLE)
                .venueId(10L)
                .build();
        given(reservationConcertQueryPort.findConcertSchedule(query.concertId(), query.date()))
                .willReturn(schedule);
        ReservationVenueSnapShot venue = ReservationVenueSnapShot.builder()
                .id(10L)
                .name("인천 아시아드 주경기장")
                .totalSeats(3)
                .seats(List.of(
                        ReservationVenueSnapShot.VenueSeatInfo.builder()
                                .id(1L)
                                .row(1)
                                .seatNumber(1)
                                .seatType(SeatType.STANDARD)
                                .build(),
                        ReservationVenueSnapShot.VenueSeatInfo.builder()
                                .id(2L)
                                .row(1)
                                .seatNumber(2)
                                .seatType(SeatType.STANDARD)
                                .build(),
                        ReservationVenueSnapShot.VenueSeatInfo.builder()
                                .id(3L)
                                .row(1)
                                .seatNumber(3)
                                .seatType(SeatType.STANDARD)
                                .build()
                ))
                .build();
        given(reservationVenueQueryPort.findVenueWithSeats(schedule.venueId()))
                .willReturn(venue);

        // 좌석 1이 예약된 상황
        List<ReservationDetail> reservationDetails = List.of(
                ReservationDetail.create(1L, 100L, 5L, 1L)
        );
        List<Reservation> reservations = List.of(
                Reservation.create(100L, 1000L, 5L, ReservationStatus.CONFIRMED,
                        LocalDateTime.now(), LocalDateTime.now(), reservationDetails)
        );
        given(reservationRepositoryPort.findReservationsByDetailScheduleId(schedule.id()))
                .willReturn(reservations);

        // when
        GetAvailableSeatListResult result = getAvailableSeatListService.execute(query);

        // then
        assertThat(result).isNotNull();
        assertThat(result.concertId()).isEqualTo(query.concertId());
        assertThat(result.concertScheduleId()).isEqualTo(schedule.id());
        assertThat(result.date()).isEqualTo(query.date());
        assertThat(result.availableSeats()).hasSize(2);
        assertThat(result.isAllReserved()).isFalse();

        // 예약되지 않은 좌석만 포함되어야 함 (좌석 2, 3)
        List<Long> availableSeatIds = result.availableSeats().stream()
                .map(seat -> seat.seatId())
                .toList();
        assertThat(availableSeatIds).containsExactlyInAnyOrder(2L, 3L);
        assertThat(availableSeatIds).doesNotContain(1L);
    }
}
