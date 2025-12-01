package com.roovies.concertreservation.ranking.application.port.out;

import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;

import java.util.List;

/**
 * 랭킹 캐시 포트.
 * <p>
 * Redis Sorted Set을 통한 랭킹 저장 및 조회를 추상화한다.
 */
public interface RankingCachePort {

    /**
     * 실시간 랭킹에 스케줄의 결제 건수를 증가시킨다.
     *
     * @param scheduleId 스케줄 ID
     */
    void incrementRealtimeRanking(Long scheduleId);

    /**
     * 실시간 랭킹 Top 5를 조회한다.
     *
     * @return 실시간 랭킹 목록 (1-5위)
     */
    List<ConcertRanking> getRealtimeTop5();

    /**
     * 주간 랭킹을 저장한다.
     *
     * @param rankings 주간 랭킹 목록
     */
    void saveWeeklyRanking(List<ConcertRanking> rankings);

    /**
     * 주간 랭킹 Top 5를 조회한다.
     *
     * @return 주간 랭킹 목록 (1-5위)
     */
    List<ConcertRanking> getWeeklyTop5();

    /**
     * 주간 랭킹 캐시를 초기화한다.
     */
    void clearWeeklyRanking();
}