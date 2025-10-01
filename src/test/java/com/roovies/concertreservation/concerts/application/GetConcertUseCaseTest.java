package com.roovies.concertreservation.concerts.application;

import com.roovies.concertreservation.concerts.application.dto.result.GetConcertResult;
import com.roovies.concertreservation.concerts.application.port.out.ConcertVenueGatewayPort;
import com.roovies.concertreservation.concerts.application.service.query.GetConcertService;
import com.roovies.concertreservation.concerts.application.port.out.ConcertQueryRepositoryPort;
import com.roovies.concertreservation.concerts.domain.entity.Concert;
import com.roovies.concertreservation.concerts.domain.entity.ConcertSchedule;
import com.roovies.concertreservation.concerts.domain.enums.ScheduleStatus;
import com.roovies.concertreservation.concerts.domain.external.ExternalVenue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
public class GetConcertUseCaseTest {

    @Mock
    private ConcertQueryRepositoryPort concertQueryRepositoryPort;

    @Mock
    private ConcertVenueGatewayPort concertVenueGatewayPort;

    @Mock
    private Clock clock;

    @InjectMocks
    private GetConcertService getConcertService;

    @Test
    void 존재하지_않는_콘서트ID로_조회시_예외가_발생해야_한다() {
        // given
        Long invalidConcertId = 999L;
        given(concertQueryRepositoryPort.findByIdWithSchedules(invalidConcertId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> getConcertService.findById(invalidConcertId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessage("콘서트를 찾을 수 없습니다.");
    }

    @Test
    void 콘서트ID가_null일_경우_예외가_발생해야_한다() {
        // given
        Long invalidConcertId = null;

        // when & then
        assertThatThrownBy(() -> getConcertService.findById(invalidConcertId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 콘서트ID입니다.");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0})
    void 콘서트ID가_음수_또는_0일_경우_예외가_발생해야_한다(Long invalidConcertId) {
        // given: parameter

        // when & then
        assertThatThrownBy(() -> getConcertService.findById(invalidConcertId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 콘서트ID입니다.");
    }

    @Test
    void 유효한_콘서트ID로_조회시_상세정보가_정상적으로_조회되어야_한다() {
        // given
        LocalDate today = LocalDate.of(2025, 9, 2);
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 3);
        LocalDateTime createdAt = LocalDateTime.of(2025, 9, 1, 0, 0);

        // Clock Mocking
        Clock fixedClock = Clock.fixed(today.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        given(clock.instant()).willReturn(fixedClock.instant());
        given(clock.getZone()).willReturn(fixedClock.getZone());

        // 콘서트 정보 Mocking
        Concert concert = Concert.create(
                1L,
                "흠뻑쇼",
                "흠뻑쇼 설명",
                70_000,
                startDate,
                endDate,
                createdAt,
                null
        );

        List<ConcertSchedule> schedules = new ArrayList<>(Arrays.asList(
                ConcertSchedule.create(1L, 1L, LocalDate.of(2025, 9, 1), 100, 100, ScheduleStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, 1L, LocalDate.of(2025, 9, 2), 100, 100, ScheduleStatus.AVAILABLE, 5L),
                ConcertSchedule.create(1L, 1L, LocalDate.of(2025, 9, 3), 100, 100, ScheduleStatus.AVAILABLE, 5L)
        ));
        concert.setSchedules(schedules);

        given(concertQueryRepositoryPort.findByIdWithSchedules(1L))
                .willReturn(Optional.of(concert));

        // 공연장 정보 Mocking
        ExternalVenue externalVenue = ExternalVenue.builder()
                .id(5L)
                .name("인천 아시아드 주경기장")
                .totalSeats(1000)
                .build();

        Long venueId = concert.getSchedule(concert.getStartDate()).getVenueId();

        given(concertVenueGatewayPort.findVenueById(venueId))
                .willReturn(externalVenue);

        // when
        GetConcertResult result = getConcertService.findById(1L);

        // then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("흠뻑쇼");
        assertThat(result.description()).isEqualTo("흠뻑쇼 설명");
        assertThat(result.minPrice()).isEqualTo(70_000);
        assertThat(result.startDate()).isEqualTo(startDate);
        assertThat(result.endDate()).isEqualTo(endDate);
        assertThat(result.status()).isEqualTo(concert.getStatus(today));
        assertThat(result.venueName()).isEqualTo("인천 아시아드 주경기장");
        assertThat(result.createdAt()).isEqualTo(createdAt);
        assertThat(result.updatedAt()).isNull();
    }


}
