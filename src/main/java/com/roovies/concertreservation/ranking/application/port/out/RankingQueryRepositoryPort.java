package com.roovies.concertreservation.ranking.application.port.out;

import com.roovies.concertreservation.ranking.application.dto.query.GetWeeklyPaymentCountQuery;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;

import java.util.List;

/**
 * 랭킹 조회 레포지토리 포트.
 * <p>
 * DB에서 주간 랭킹을 계산하기 위한 데이터를 조회한다.
 */
public interface RankingQueryRepositoryPort {

    /**
     * 주간 결제 건수를 기준으로 상위 5개 스케줄을 조회한다.
     *
     * @param query 주간 결제 건수 조회 쿼리
     * @return 주간 랭킹 목록 (1-5위)
     */
    List<ConcertRanking> findWeeklyTop5(GetWeeklyPaymentCountQuery query);

    /**
     * 스케줄 ID로 콘서트 정보를 조회한다.
     *
     * @param scheduleId 스케줄 ID
     * @return 콘서트 ID와 제목
     */
    ScheduleConcertInfo findConcertInfoByScheduleId(Long scheduleId);

    /**
     * 스케줄의 콘서트 정보.
     */
    record ScheduleConcertInfo(
            Long scheduleId,
            Long concertId,
            String concertTitle
    ) {
    }
}