package com.roovies.concertreservation.ranking.application.unit;

import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;
import com.roovies.concertreservation.ranking.application.port.out.RankingCachePort;
import com.roovies.concertreservation.ranking.application.port.out.RankingQueryRepositoryPort;
import com.roovies.concertreservation.ranking.application.service.RealtimeRankingService;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import com.roovies.concertreservation.ranking.domain.vo.RankingType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

/**
 * RealtimeRankingService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
public class RealtimeRankingServiceTest {

    @Mock
    private RankingCachePort rankingCachePort;

    @Mock
    private RankingQueryRepositoryPort rankingQueryRepositoryPort;

    @InjectMocks
    private RealtimeRankingService realtimeRankingService;

    @Test
    void 실시간_랭킹_조회시_Redis에서_Top5를_반환해야_한다() {
        // given
        List<ConcertRanking> mockRankings = Arrays.asList(
                ConcertRanking.createRealtime(1L, 10L, "흠뻑쇼", 100L, 1),
                ConcertRanking.createRealtime(2L, 20L, "아이유 콘서트", 90L, 2),
                ConcertRanking.createRealtime(3L, 30L, "싸이 콘서트", 80L, 3),
                ConcertRanking.createRealtime(4L, 40L, "BTS 콘서트", 70L, 4),
                ConcertRanking.createRealtime(5L, 50L, "블랙핑크 콘서트", 60L, 5)
        );

        given(rankingCachePort.getRealtimeTop5()).willReturn(mockRankings);

        // when
        List<ConcertRankingResult> results = realtimeRankingService.getRealtimeRanking();

        // then
        assertThat(results).hasSize(5);
        assertThat(results.get(0).scheduleId()).isEqualTo(1L);
        assertThat(results.get(0).concertTitle()).isEqualTo("흠뻑쇼");
        assertThat(results.get(0).paymentCount()).isEqualTo(100L);
        assertThat(results.get(0).rank()).isEqualTo(1);
        assertThat(results.get(0).rankingType()).isEqualTo(RankingType.REALTIME);

        verify(rankingCachePort, times(1)).getRealtimeTop5();
    }

    @Test
    void 실시간_랭킹_업데이트시_Redis_스코어가_증가해야_한다() {
        // given
        Long scheduleId = 1L;
        willDoNothing().given(rankingCachePort).incrementRealtimeRanking(scheduleId);

        // when
        realtimeRankingService.updateRealtimeRanking(scheduleId);

        // then
        verify(rankingCachePort, times(1)).incrementRealtimeRanking(scheduleId);
    }

    @Test
    void 주간_랭킹_업데이트_호출시_예외가_발생해야_한다() {
        // when & then
        assertThatThrownBy(() -> realtimeRankingService.updateWeeklyRanking())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("주간 랭킹 업데이트는 WeeklyRankingService에서 처리합니다.");
    }

    @Test
    void 실시간_랭킹이_비어있을_경우_빈_리스트를_반환해야_한다() {
        // given
        given(rankingCachePort.getRealtimeTop5()).willReturn(List.of());

        // when
        List<ConcertRankingResult> results = realtimeRankingService.getRealtimeRanking();

        // then
        assertThat(results).isEmpty();
        verify(rankingCachePort, times(1)).getRealtimeTop5();
    }
}