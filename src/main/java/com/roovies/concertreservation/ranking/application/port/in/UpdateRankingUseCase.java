package com.roovies.concertreservation.ranking.application.port.in;

/**
 * 랭킹 업데이트 유스케이스.
 */
public interface UpdateRankingUseCase {
    /**
     * 결제 완료 이벤트를 받아 실시간 랭킹을 업데이트한다.
     *
     * @param scheduleId 스케줄 ID
     */
    void updateRealtimeRanking(Long scheduleId);

    /**
     * 주간 랭킹을 재계산하여 업데이트한다.
     */
    void updateWeeklyRanking();
}