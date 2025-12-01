package com.roovies.concertreservation.ranking.application.port.in;

import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;

import java.util.List;

/**
 * 주간 랭킹 조회 유스케이스.
 */
public interface GetWeeklyRankingUseCase {
    /**
     * 주간 랭킹(일주일 기준) Top 5를 조회한다.
     *
     * @return 주간 랭킹 목록 (1-5위)
     */
    List<ConcertRankingResult> getWeeklyRanking();
}