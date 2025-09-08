package com.roovies.concertreservation.concerts.domain;

import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.domain.entity.ConcertSchedule;
import com.roovies.concertreservation.concerts.domain.enums.ConcertStatus;
import com.roovies.concertreservation.concerts.domain.enums.ReservationStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ConcertTest {
    @Test
    void 현재_날짜가_콘서트의_시작일보다_빠른_경우_콘서트의_상태는_PREPARE이어야_한다() {
        // given
        LocalDate now = LocalDate.of(2025, 8, 31);
        Concert concert = Concert.create(
                1L,
                "흠뻑쇼",
                "흠뻑쇼 설명",
                70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 5),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );

        // when & then
        assertThat(concert.getStatus(now)).isEqualTo(ConcertStatus.PREPARE);
    }

    @Test
    void 현재_날짜가_콘서트의_시작일과_종료일_사이에_있는_경우_콘서트의_상태는_ONGOING이어야_한다() {
        // given
        LocalDate now = LocalDate.of(2025, 9, 3);
        Concert concert = Concert.create(
                1L,
                "흠뻑쇼",
                "흠뻑쇼 설명",
                70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 5),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );

        // when & then
        assertThat(concert.getStatus(now)).isEqualTo(ConcertStatus.ONGOING);
    }

    @Test
    void 현재_날짜가_콘서트의_종료일보다_늦는_경우_콘서트의_상태는_ENDED이어야_한다() {
        // given
        LocalDate now = LocalDate.of(2025, 9, 6);
        Concert concert = Concert.create(
                1L,
                "흠뻑쇼",
                "흠뻑쇼 설명",
                70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 5),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );

        // when & then
        assertThat(concert.getStatus(now)).isEqualTo(ConcertStatus.ENDED);
    }

    @Test
    void 콘서트_생성_후_일정_목록을_설정할_수_있다() {
        // given
        // 콘서트 생성
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );

        // 상세 일정 목록 설정
        List<ConcertSchedule> schedules = List.of(
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 1), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 2), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 3), 100, 100, ReservationStatus.AVAILABLE, 5L)
        );

        // when
        concert.setSchedules(schedules);

        // then
        assertThat(concert.getSchedules()).hasSize(3);
        assertThat(concert.getSchedules())
                // concert.getSchedules()의 결과에서 날짜만 추출
                .extracting(concertSchedule -> concertSchedule.getDate())
                // 컬렉션이 지정된 요소들을 정확히 같은 순서로 포함하는지 검증
                .containsExactly(
                        LocalDate.of(2025, 9, 1),
                        LocalDate.of(2025, 9, 2),
                        LocalDate.of(2025, 9, 3)
                );
    }

    @Test
    void 콘서트에_일정목록을_설정할_때_시작일과_종료일_범위에_벗어나는_일정이_있을_경우_예외가_발생해야_한다() {
        // given
        // 콘서트 생성
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );

        // 상세 일정 목록 설정
        List<ConcertSchedule> invalidSchedules = List.of(
                ConcertSchedule.create(1L, LocalDate.of(2025, 8, 31), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 2), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 3), 100, 100, ReservationStatus.AVAILABLE, 5L)
        );

        // when & then
        assertThatThrownBy(() -> concert.setSchedules(invalidSchedules))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("콘서트의 상세일정은 시작일과 종료일 범위 내에 있어야 합니다.");
    }

    @Test
    void 조회한_콘서트_일정목록_컬렉션에_직접적으로_일정을_추가할_수_없어야_한다() {
        // given
        // 콘서트 생성
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );

        // 상세 일정 목록 설정
        List<ConcertSchedule> validSchedules = new ArrayList<>(Arrays.asList(
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 1), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 2), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 3), 100, 100, ReservationStatus.AVAILABLE, 5L)
        ));

        concert.setSchedules(validSchedules);
        List<ConcertSchedule> schedules = concert.getSchedules();

        // when & then
        assertThatThrownBy(() -> schedules.add(ConcertSchedule.create(1L, LocalDate.of(2025, 9, 4), 100, 100, ReservationStatus.AVAILABLE, 5L)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 콘서트에_일정을_추가할_때_시작일과_종료일_범위에_벗어날_경우_예외가_발생해야_한다() {
        // given
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );
        ConcertSchedule invalidConcerSchedule = ConcertSchedule.create(1L, LocalDate.of(2025, 8, 31), 100, 100, ReservationStatus.AVAILABLE, 5L); // 시작일 이전

        // when & then
        assertThatThrownBy(() -> concert.addSchedule(invalidConcerSchedule))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("콘서트의 상세일정은 시작일과 종료일 범위 내에 있어야 합니다.");
    }

    @Test
    void 콘서트에_일정을_추가할_때_날짜를_기준으로_정렬되어_추가되어야_한다() {
        // given
        // 기존 콘서트 정보
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 5),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );
        List<ConcertSchedule> existedSchedules = new ArrayList<>(Arrays.asList(
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 1), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 3), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 5), 100, 100, ReservationStatus.AVAILABLE, 5L)
        ));
        concert.setSchedules(existedSchedules);

        // 새 일정
        ConcertSchedule newSchedule = ConcertSchedule.create(1L, LocalDate.of(2025, 9, 2), 100, 100, ReservationStatus.AVAILABLE, 5L);

        // when
        concert.addSchedule(newSchedule);

        // then
        List<ConcertSchedule> schedules = concert.getSchedules();
        List<LocalDate> dates = schedules.stream()
                .map(schedule -> schedule.getDate())
                .toList();

        assertThat(dates).isSorted(); // 오름차순 정렬 여부 검증
        assertThat(dates).hasSize(4);
    }

    @Test
    void 특정_날짜로_콘서트의_스케줄_정보를_조회할_수_있다() {
        // given
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );
        List<ConcertSchedule> existedSchedules = new ArrayList<>(Arrays.asList(
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 1), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 2), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 3), 100, 100, ReservationStatus.AVAILABLE, 5L)
        ));
        concert.setSchedules(existedSchedules);

        // when
        LocalDate date = LocalDate.of(2025, 9, 2);
        ConcertSchedule result = concert.getSchedule(date);

        // then
        assertThat(result.getDate()).isEqualTo(date);
    }

    @Test
    void 특정_날짜로_콘서트의_스케줄_정보를_조회할_때_일정이_존재하지_않으면_예외가_발생해야_한다() {
        // given
        Concert concert = Concert.create(
                1L, "흠뻑쇼", "설명", 70_000,
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 3),
                LocalDateTime.of(2025, 9, 1, 0, 0),
                null
        );
        List<ConcertSchedule> existedSchedules = new ArrayList<>(Arrays.asList(
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 1), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 2), 100, 100, ReservationStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, LocalDate.of(2025, 9, 3), 100, 100, ReservationStatus.AVAILABLE, 5L)
        ));
        concert.setSchedules(existedSchedules);

        // when
        LocalDate date = LocalDate.of(2025, 9, 4);

        // then
        assertThatThrownBy(() -> concert.getSchedule(date))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 날짜의 콘서트 일정이 존재하지 않습니다.");
    }



    //    @ParameterizedTest
//    @ValueSource(ints = {101, 102})
//    void 조회된_콘서트의_예약_가능한_좌석의_수가_총_좌석의_수보다_많으면_예외가_발생한다(int availableSeats) {
//        // given
//        Long concertId = 1L;
//        Concert concert = Concert.builder()
//                .id(concertId)
//                .title("흠뻑쇼")
//                .description("싸이가 주최하는 흠뻑쇼")
//                .minPrice(17_800L)
//                .startDate(LocalDate.of(2025, 9, 1))
//                .endDate(LocalDate.of(2025, 9, 5))
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .concertHallName("인천 아시아드 경기장")
//                .totalSeats(100)
//                .build();
//
//        // when & then
//        assertThatThrownBy(() -> concert.setAvailableSeats(availableSeats))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessage("예약 가능한 좌석은 총 좌석의 수보다 많을 수 없습니다.");
//    }
//
//    @ParameterizedTest
//    @ValueSource(ints = {99, 100})
//    void 조회된_콘서트의_예약_가능한_좌석의_수는_총_좌석의_수보다_같거나_작아야_한다(int availableSeats) {
//        // given
//        Long concertId = 1L;
//        Concert concert = Concert.builder()
//                .id(concertId)
//                .title("흠뻑쇼")
//                .description("싸이가 주최하는 흠뻑쇼")
//                .minPrice(17_800L)
//                .startDate(LocalDate.of(2025, 9, 1))
//                .endDate(LocalDate.of(2025, 9, 5))
//                .createdAt(LocalDateTime.now())
//                .updatedAt(LocalDateTime.now())
//                .concertHallName("인천 아시아드 경기장")
//                .totalSeats(100)
//                .build();
//
//        // when
//        concert.setAvailableSeats(availableSeats);
//
//        // then
//        assertThat(concert.getTotalSeats()).isEqualTo(100);
//        assertThat(concert.getAvailableSeats()).isLessThanOrEqualTo(availableSeats);
//    }
}
