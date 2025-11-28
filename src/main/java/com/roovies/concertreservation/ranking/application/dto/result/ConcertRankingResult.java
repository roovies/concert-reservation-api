package com.roovies.concertreservation.ranking.application.dto.result;

import com.roovies.concertreservation.ranking.domain.vo.RankingType;
import lombok.Builder;

/**
 * 콘서트 랭킹 조회 결과.
 */
@Builder
public record ConcertRankingResult(
        Long scheduleId,
        Long concertId,
        String concertTitle,
        Long paymentCount,
        Integer rank,
        RankingType rankingType
) {
}