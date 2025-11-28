package com.roovies.concertreservation.ranking.application.port.in;

import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;

import java.util.List;

/**
 * 실시간 랭킹 조회 유스케이스.
 */
public interface GetRealtimeRankingUseCase {
    /**
     * 실시간 랭킹(최근 24시간 기준) Top 5를 조회한다.
     *
     * @return 실시간 랭킹 목록 (1-5위)
     */
    List<ConcertRankingResult> getRealtimeRanking();
}