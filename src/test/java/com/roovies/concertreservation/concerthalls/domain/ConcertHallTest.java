package com.roovies.concertreservation.concerthalls.domain;

import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHall;
import com.roovies.concertreservation.concerthalls.domain.entity.ConcertHallSeat;
import com.roovies.concertreservation.concerthalls.domain.enums.SeatType;
import com.roovies.concertreservation.concerthalls.domain.vo.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

public class ConcertHallTest {
    @Test
    void 공연장을_정상적으로_생성할_수_있다() {
        // given
        Long concertHallId = 1L;
        String name = "인천 아시아드 주경기장";
        int totalSeats = 1000;
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 7, 10, 0);
        LocalDateTime updatedAt = null;

        // when
        ConcertHall result = ConcertHall.create(concertHallId, name, totalSeats, createdAt, updatedAt);

        // then
        assertThat(result.getId()).isEqualTo(concertHallId);
        assertThat(result.getName()).isEqualTo(name);
        assertThat(result.getTotalSeats()).isEqualTo(totalSeats);
        assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        assertThat(result.getUpdatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void 공연장에_좌석_목록을_설정할_수_있다() {
        // given
        // 공연장 정보
        Long concertHallId = 1L;
        String name = "인천 아시아드 주경기장";
        int totalSeats = 3;
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 7, 10, 0);
        LocalDateTime updatedAt = null;
        ConcertHall result = ConcertHall.create(concertHallId, name, totalSeats, createdAt, updatedAt);

        // 좌석 정보
        List<ConcertHallSeat> seats = List.of(
                ConcertHallSeat.create(1L, 1, 1, SeatType.STANDARD, new Money(10000), createdAt),
                ConcertHallSeat.create(1L, 1, 2, SeatType.STANDARD, new Money(10000), createdAt),
                ConcertHallSeat.create(1L, 1, 3, SeatType.STANDARD, new Money(10000), createdAt)
        );

        // when
        result.setSeats(seats);

        // then
        assertThat(result.getSeats()).isEqualTo(seats);
        assertThat(result.getSeats().size()).isEqualTo(totalSeats);
    }

    @Test
    void 조회한_공연장_좌석목록_컬렉션에_직접적으로_좌석을_추가할_수_없어야_한다() {
        // given
        // 공연장 생성
        Long concertHallId = 1L;
        String name = "인천 아시아드 주경기장";
        int totalSeats = 3;
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 7, 10, 0);
        LocalDateTime updatedAt = null;
        ConcertHall result = ConcertHall.create(concertHallId, name, totalSeats, createdAt, updatedAt);

        // 상세 일정 목록 설정
        List<ConcertHallSeat> seats = new ArrayList<>(Arrays.asList(
                ConcertHallSeat.create(1L, 1, 1, SeatType.STANDARD, new Money(10000), createdAt),
                ConcertHallSeat.create(1L, 1, 2, SeatType.STANDARD, new Money(10000), createdAt),
                ConcertHallSeat.create(1L, 1, 3, SeatType.STANDARD, new Money(10000), createdAt)
        ));

        result.setSeats(seats);
        List<ConcertHallSeat> resultSeats = result.getSeats();

        // when & then
        assertThatThrownBy(() -> resultSeats.add(ConcertHallSeat.create(1L, 1, 4, SeatType.STANDARD, new Money(10000), createdAt)))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
