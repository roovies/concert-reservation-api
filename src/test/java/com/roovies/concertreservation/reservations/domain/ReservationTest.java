package com.roovies.concertreservation.reservations.domain;

import com.roovies.concertreservation.reservations.domain.entity.Reservation;
import com.roovies.concertreservation.reservations.domain.entity.ReservationDetail;
import com.roovies.concertreservation.reservations.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ReservationTest {
    @Test
    void 예약을_생성할_때_예약_상세내역이_비어있으면_예외가_발생해야_한다() {
        // given
        List<ReservationDetail> details = new ArrayList<>();

        // when & then
        assertThatThrownBy(() -> Reservation.create(
                1L,
                5L,
                ReservationStatus.HOLD,
                LocalDateTime.of(2025, 9, 9, 15, 26),
                null,
                details
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("예약 상세내역은 비어있을 수 없습니다.");
    }

    @Test
    void 예약을_생성할_때_예약_상세내역이_null이면_예외가_발생해야_한다() {
        // given
        List<ReservationDetail> details = null;

        // when & then
        assertThatThrownBy(() -> Reservation.create(
                1L,
                5L,
                ReservationStatus.HOLD,
                LocalDateTime.of(2025, 9, 9, 15, 26),
                null,
                details
        ))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("예약 상세내역은 비어있을 수 없습니다.");
    }

    @Test
    void 새로_생성된_예약의_상태는_HOLD이고_수정일이_null이어야_한다() {
        // given
        List<ReservationDetail> details = new ArrayList<>();
        details.add(ReservationDetail.create(
                1L,
                1L,
                2L,
                4L
        ));

        // when
        Reservation reservation = Reservation.create(
                1L,
                5L,
                ReservationStatus.HOLD,
                LocalDateTime.of(2025, 9, 9, 15, 26),
                null,
                details
        );

        // then
        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.HOLD);
        assertThat(reservation.getUpdatedAt())
                .isNull();
    }

    @Test
    void 예약을_생성할_때_모든_예약_상세내역의_reservationId는_생성하려는_예약ID와_동일해야_한다() {
        // given
        List<ReservationDetail> details = new ArrayList<>();
        details.add(ReservationDetail.create(
                1L,
                10L,
                2L,
                4L
        ));
        // 다른 예약ID를 가진 상세 내역
        details.add(ReservationDetail.create(
                1L,
                20L,
                2L,
                4L
        ));

        // when & then
        assertThatThrownBy(() -> Reservation.create(
                10L,
                5L,
                ReservationStatus.HOLD,
                LocalDateTime.of(2025, 9, 9, 15, 26),
                null,
                details
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 예약에 대한 예약 상세내역만 등록할 수 있습니다.");
    }

    @Test
    void 조회한_예약_상세내역_목록은_읽기_전용이어야_한다() {
        // given
        List<ReservationDetail> details = new ArrayList<>();
        details.add(ReservationDetail.create(
                1L,
                5L,
                2L,
                4L
        ));

        Reservation reservation = Reservation.create(
                5L,
                1L,
                ReservationStatus.HOLD,
                LocalDateTime.of(2025, 9, 9, 15, 26),
                null,
                details
        );

        // when
        List<ReservationDetail> result = reservation.getDetails();

        // then
        ReservationDetail newReservationDetail = ReservationDetail.create(
                2L,
                5L,
                2L,
                5L
        );
        assertThatThrownBy(() -> result.add(newReservationDetail))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 예약_상세내역의_스케줄ID와_좌석ID의_조합은_유니크해야_한다() {
        // given
        List<ReservationDetail> details = new ArrayList<>();
        details.add(ReservationDetail.create(
                1L,
                5L,
                2L,
                5L
        ));
        details.add(ReservationDetail.create(
                2L,
                5L,
                2L,
                5L
        ));

        // when & then
        assertThatThrownBy(() -> Reservation.create(
                5L,
                1L,
                ReservationStatus.HOLD,
                LocalDateTime.of(2025, 9, 9, 15, 26),
                null,
                details
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("동일한 날짜의 같은 좌석은 중복 예약할 수 없습니다.");
    }
}
