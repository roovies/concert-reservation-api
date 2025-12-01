package com.roovies.concertreservation.ranking.application.unit;

import com.roovies.concertreservation.ranking.application.dto.query.GetWeeklyPaymentCountQuery;
import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;
import com.roovies.concertreservation.ranking.application.port.out.RankingCachePort;
import com.roovies.concertreservation.ranking.application.port.out.RankingQueryRepositoryPort;
import com.roovies.concertreservation.ranking.application.service.WeeklyRankingService;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import com.roovies.concertreservation.ranking.domain.vo.RankingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * WeeklyRankingService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
public class WeeklyRankingServiceTest {

    @Mock
    private RankingCachePort rankingCachePort;

    @Mock
    private RankingQueryRepositoryPort rankingQueryRepositoryPort;

    @InjectMocks
    private WeeklyRankingService weeklyRankingService;

    @Test
    void 주간_랭킹_조회시_Redis에서_Top5를_반환해야_한다() {
        // given
        List<ConcertRanking> mockRankings = Arrays.asList(
                ConcertRanking.createWeekly(1L, 10L, "흠뻑쇼", 500L, 1),
                ConcertRanking.createWeekly(2L, 20L, "아이유 콘서트", 450L, 2),
                ConcertRanking.createWeekly(3L, 30L, "싸이 콘서트", 400L, 3),
                ConcertRanking.createWeekly(4L, 40L, "BTS 콘서트", 350L, 4),
                ConcertRanking.createWeekly(5L, 50L, "블랙핑크 콘서트", 300L, 5)
        );

        given(rankingCachePort.getWeeklyTop5()).willReturn(mockRankings);

        // when
        List<ConcertRankingResult> results = weeklyRankingService.getWeeklyRanking();

        // then
        assertThat(results).hasSize(5);
        assertThat(results.get(0).scheduleId()).isEqualTo(1L);
        assertThat(results.get(0).concertTitle()).isEqualTo("흠뻑쇼");
        assertThat(results.get(0).paymentCount()).isEqualTo(500L);
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(0).rankingType()).isEqualTo(RankingType.WEEKLY);

        verify(rankingCachePort, times(1)).getWeeklyTop5();
    }

    @Test
    void 주간_랭킹_업데이트시_DB에서_조회하고_Redis에_저장해야_한다() {
        // given
        List<ConcertRanking> dbRankings = Arrays.asList(
                ConcertRanking.createWeekly(1L, 10L, "흠뻑쇼", 500L, 1),
                ConcertRanking.createWeekly(2L, 20L, "아이유 콘서트", 450L, 2)
        );

        given(rankingQueryRepositoryPort.findWeeklyTop5(any(GetWeeklyPaymentCountQuery.class)))
                .willReturn(dbRankings);

        willDoNothing().given(rankingCachePort).saveWeeklyRanking(any());

        // when
        weeklyRankingService.updateWeeklyRanking();

        // then
        ArgumentCaptor<GetWeeklyPaymentCountQuery> queryCaptor =
                ArgumentCaptor.forClass(GetWeeklyPaymentCountQuery.class);
        verify(rankingQueryRepositoryPort, times(1)).findWeeklyTop5(queryCaptor.capture());

        GetWeeklyPaymentCountQuery capturedQuery = queryCaptor.getValue();
        assertThat(capturedQuery.startDate()).isNotNull();
        assertThat(capturedQuery.endDate()).isNotNull();
        assertThat(capturedQuery.endDate()).isAfter(capturedQuery.startDate());

        verify(rankingCachePort, times(1)).saveWeeklyRanking(dbRankings);
    }

    @Test
    void 실시간_랭킹_업데이트_호출시_예외가_발생해야_한다() {
        // when & then
        assertThatThrownBy(() -> weeklyRankingService.updateRealtimeRanking(1L))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("실시간 랭킹 업데이트는 RealtimeRankingService에서 처리합니다.");
    }

    @Test
    void 주간_랭킹이_비어있을_경우_빈_리스트를_반환해야_한다() {
        // given
        given(rankingCachePort.getWeeklyTop5()).willReturn(List.of());

        // when
        List<ConcertRankingResult> results = weeklyRankingService.getWeeklyRanking();

        // then
        assertThat(results).isEmpty();
        verify(rankingCachePort, times(1)).getWeeklyTop5();
    }

    @Test
    void 주간_랭킹_업데이트시_7일전부터_현재까지_데이터를_조회해야_한다() {
        // given
        List<ConcertRanking> dbRankings = List.of(
                ConcertRanking.createWeekly(1L, 10L, "콘서트", 100L, 1)
        );

        given(rankingQueryRepositoryPort.findWeeklyTop5(any(GetWeeklyPaymentCountQuery.class)))
                .willReturn(dbRankings);

        // when
        weeklyRankingService.updateWeeklyRanking();

        // then
        ArgumentCaptor<GetWeeklyPaymentCountQuery> queryCaptor =
                ArgumentCaptor.forClass(GetWeeklyPaymentCountQuery.class);
        verify(rankingQueryRepositoryPort).findWeeklyTop5(queryCaptor.capture());

        GetWeeklyPaymentCountQuery query = queryCaptor.getValue();
        LocalDateTime now = LocalDateTime.now();

        // 7일 전부터 현재까지 조회하는지 확인
        assertThat(query.startDate()).isBefore(query.endDate());
        assertThat(query.endDate()).isAfterOrEqualTo(now.minusMinutes(1));
    }
}