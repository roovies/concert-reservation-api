package com.roovies.concertreservation.ranking.domain;

import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import com.roovies.concertreservation.ranking.domain.vo.RankingType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ConcertRanking 도메인 엔티티 테스트.
 */
public class ConcertRankingTest {

    @Test
    void 실시간_랭킹을_생성할_수_있다() {
        // given
        Long scheduleId = 1L;
        Long concertId = 10L;
        String concertTitle = "흠뻑쇼";
        Long paymentCount = 100L;
        Integer rank = 1;

        // when
        ConcertRanking ranking = ConcertRanking.createRealtime(
                scheduleId,
                concertId,
                concertTitle,
                paymentCount,
                rank
        );

        // then
        assertThat(ranking).isNotNull();
        assertThat(ranking.getScheduleId()).isEqualTo(scheduleId);
        assertThat(ranking.getConcertId()).isEqualTo(concertId);
        assertThat(ranking.getConcertTitle()).isEqualTo(concertTitle);
        assertThat(ranking.getPaymentCount()).isEqualTo(paymentCount);
        assertThat(ranking.getRank()).isEqualTo(rank);
        assertThat(ranking.getRankingType()).isEqualTo(RankingType.REALTIME);
    }

    @Test
    void 주간_랭킹을_생성할_수_있다() {
        // given
        Long scheduleId = 2L;
        Long concertId = 20L;
        String concertTitle = "아이유 콘서트";
        Long paymentCount = 500L;
        Integer rank = 2;

        // when
        ConcertRanking ranking = ConcertRanking.createWeekly(
                scheduleId,
                concertId,
                concertTitle,
                paymentCount,
                rank
        );

        // then
        assertThat(ranking).isNotNull();
        assertThat(ranking.getScheduleId()).isEqualTo(scheduleId);
        assertThat(ranking.getConcertId()).isEqualTo(concertId);
        assertThat(ranking.getConcertTitle()).isEqualTo(concertTitle);
        assertThat(ranking.getPaymentCount()).isEqualTo(paymentCount);
        assertThat(ranking.getRank()).isEqualTo(rank);
        assertThat(ranking.getRankingType()).isEqualTo(RankingType.WEEKLY);
    }

    @Test
    void 랭킹은_불변_객체이다() {
        // given
        ConcertRanking ranking = ConcertRanking.createRealtime(
                1L, 10L, "콘서트", 100L, 1
        );

        // when & then
        // ConcertRanking은 setter가 없으므로 불변 객체임을 확인
        assertThat(ranking.getClass().getDeclaredMethods())
                .noneMatch(method -> method.getName().startsWith("set"));
    }
}