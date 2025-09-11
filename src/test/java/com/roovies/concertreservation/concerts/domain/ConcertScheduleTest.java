package com.roovies.concertreservation.concerts.domain;

import com.roovies.concertreservation.concerts.domain.entity.ConcertSchedule;
import com.roovies.concertreservation.concerts.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

public class ConcertScheduleTest {

    @Test
    void 콘서트_스케줄을_정상적으로_생성할_수_있다() {
        // given
        Long scheduleId = 1L;
        LocalDate date = LocalDate.of(2025, 9, 7);
        int totalSeats = 100;
        int availableSeats = 100;
        ReservationStatus reservationStatus = ReservationStatus.AVAILABLE;
        Long concertHallId = 10L;

        // when
        ConcertSchedule schedule = ConcertSchedule.create(scheduleId, date, totalSeats, availableSeats, reservationStatus, concertHallId);

        // then
        assertThat(schedule.getId()).isEqualTo(scheduleId);
        assertThat(schedule.getDate()).isEqualTo(date);
        assertThat(schedule.getTotalSeats()).isEqualTo(totalSeats);
        assertThat(schedule.getAvailableSeats()).isEqualTo(availableSeats);
        assertThat(schedule.getConcertHallId()).isEqualTo(concertHallId);
    }

    @Test
    void 생성된_스케줄의_초기_예약_가능_좌석의_수는_전체_좌석의_수와_동일해야_한다() {
        // given
        Long scheduleId = 1L;
        LocalDate date = LocalDate.of(2025, 9, 7);
        int totalSeats = 100;
        int availableSeats = 100;
        ReservationStatus reservationStatus = ReservationStatus.AVAILABLE;
        Long concertHallId = 10L;

        // when
        ConcertSchedule schedule = ConcertSchedule.create(scheduleId, date, totalSeats, availableSeats, reservationStatus, concertHallId);

        // then
        assertThat(schedule.getAvailableSeats()).isEqualTo(totalSeats);
    }

    @Test
    void 스케줄의_예약_가능한_좌석의_수는_전체_좌석의_수보다_클_경우_예외가_발생해야_한다() {
        // given
        Long scheduleId = 1L;
        LocalDate date = LocalDate.of(2025, 9, 7);
        int totalSeats = 100;
        int availableSeats = 101;
        ReservationStatus reservationStatus = ReservationStatus.AVAILABLE;
        Long concertHallId = 10L;

        // when & then
        assertThatThrownBy(() -> ConcertSchedule.create(scheduleId, date, totalSeats, availableSeats, reservationStatus, concertHallId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("예약 가능한 좌석의 수는 전체 좌석 수보다 많을 수 없습니다.");
    }

    @Test
    void 스케줄의_예약_가능한_좌석의_수를_감소시킬_수_있다() {
        // given
        ConcertSchedule schedule = ConcertSchedule.create(
                1L, LocalDate.of(2025, 9, 7), 100, 100, ReservationStatus.AVAILABLE, 10L
        );

        // when
        int availableSeats = schedule.decreaseAvailableSeats(5);

        // then
        assertThat(availableSeats).isEqualTo(95);
        assertThat(schedule.getAvailableSeats()).isEqualTo(95);
    }

    @Test
    void 예약_가능한_좌석의_수를_감소시켰을_때_결과값이_0보다_작을_경우_예외가_발생해야_한다() {
        // given
        ConcertSchedule schedule = ConcertSchedule.create(
                1L, LocalDate.of(2025, 9, 7), 100, 1, ReservationStatus.AVAILABLE, 10L
        );

        // when & then
        assertThatThrownBy(() -> schedule.decreaseAvailableSeats(2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잔여 좌석의 수보다 예약하는 좌석의 수가 더 클 수 없습니다.");
    }

    @Test
    void 예약_가능한_좌석의_수를_감소시켰을_때_결과값이_0인_경우_예약_상태는_SOLD_OUT이어야_한다() {
        // given
        ConcertSchedule schedule = ConcertSchedule.create(
                1L, LocalDate.of(2025, 9, 7), 100, 1, ReservationStatus.AVAILABLE, 10L
        );

        // when
        int availableSeats = schedule.decreaseAvailableSeats(1);

        // then
        assertThat(availableSeats).isEqualTo(0);
        assertThat(schedule.getReservationStatus()).isEqualTo(ReservationStatus.SOLD_OUT);
    }
}
