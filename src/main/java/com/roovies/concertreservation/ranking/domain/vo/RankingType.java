package com.roovies.concertreservation.ranking.domain.vo;

/**
 * 랭킹 타입.
 */
public enum RankingType {
    /**
     * 실시간 랭킹 (최근 24시간 기준)
     */
    REALTIME,

    /**
     * 주간 랭킹 (일주일 기준)
     */
    WEEKLY
}