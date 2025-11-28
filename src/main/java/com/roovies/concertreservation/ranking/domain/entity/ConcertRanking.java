package com.roovies.concertreservation.ranking.domain.entity;

import com.roovies.concertreservation.ranking.domain.vo.RankingType;
import lombok.Builder;
import lombok.Getter;

/**
 * 콘서트 랭킹 도메인 엔티티.
 * <p>
 * 콘서트 스케줄별 랭킹 정보를 나타낸다.
 * 불변 객체로 설계되어 있다.
 */
@Getter
@Builder
public class ConcertRanking {

    private final Long scheduleId;
    private final Long concertId;
    private final String concertTitle;
    private final Long paymentCount;
    private final Integer rank;
    private final RankingType rankingType;

    /**
     * 실시간 랭킹을 생성한다.
     */
    public static ConcertRanking createRealtime(
            Long scheduleId,
            Long concertId,
            String concertTitle,
            Long paymentCount,
            Integer rank
    ) {
        return ConcertRanking.builder()
                .scheduleId(scheduleId)
                .concertId(concertId)
                .concertTitle(concertTitle)
                .paymentCount(paymentCount)
                .rank(rank)
                .rankingType(RankingType.REALTIME)
                .build();
    }

    /**
     * 주간 랭킹을 생성한다.
     */
    public static ConcertRanking createWeekly(
            Long scheduleId,
            Long concertId,
            String concertTitle,
            Long paymentCount,
            Integer rank
    ) {
        return ConcertRanking.builder()
                .scheduleId(scheduleId)
                .concertId(concertId)
                .concertTitle(concertTitle)
                .paymentCount(paymentCount)
                .rank(rank)
                .rankingType(RankingType.WEEKLY)
                .build();
    }
}