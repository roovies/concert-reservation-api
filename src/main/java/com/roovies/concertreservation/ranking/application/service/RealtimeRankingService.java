package com.roovies.concertreservation.ranking.application.service;

import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;
import com.roovies.concertreservation.ranking.application.port.in.GetRealtimeRankingUseCase;
import com.roovies.concertreservation.ranking.application.port.in.UpdateRankingUseCase;
import com.roovies.concertreservation.ranking.application.port.out.RankingCachePort;
import com.roovies.concertreservation.ranking.application.port.out.RankingQueryRepositoryPort;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 실시간 랭킹 서비스.
 * <p>
 * 최근 24시간 기준 결제 건수를 기반으로 실시간 랭킹을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Qualifier("realtimeRankingService")
public class RealtimeRankingService implements GetRealtimeRankingUseCase, UpdateRankingUseCase {

    @Qualifier("rankingRedisCache")
    private final RankingCachePort rankingCachePort;

    @Qualifier("rankingQueryRepository")
    private final RankingQueryRepositoryPort rankingQueryRepositoryPort;

    /**
     * 실시간 랭킹 Top 5를 조회한다.
     *
     * @return 실시간 랭킹 목록 (1-5위)
     */
    @Override
    public List<ConcertRankingResult> getRealtimeRanking() {
        log.info("[RealtimeRankingService] 실시간 랭킹 조회");

        List<ConcertRanking> rankings = rankingCachePort.getRealtimeTop5();

        return rankings.stream()
                .map(ranking -> ConcertRankingResult.builder()
                        .scheduleId(ranking.getScheduleId())
                        .concertId(ranking.getConcertId())
                        .concertTitle(ranking.getConcertTitle())
                        .paymentCount(ranking.getPaymentCount())
                        .rank(ranking.getRank())
                        .rankingType(ranking.getRankingType())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 결제 완료 이벤트를 받아 실시간 랭킹을 업데이트한다.
     *
     * @param scheduleId 스케줄 ID
     */
    @Override
    public void updateRealtimeRanking(Long scheduleId) {
        log.info("[RealtimeRankingService] 실시간 랭킹 업데이트 - scheduleId: {}", scheduleId);

        // Redis Sorted Set에 스케줄의 점수(결제 건수) 증가
        rankingCachePort.incrementRealtimeRanking(scheduleId);

        log.info("[RealtimeRankingService] 실시간 랭킹 업데이트 완료 - scheduleId: {}", scheduleId);
    }

    /**
     * 주간 랭킹 업데이트는 이 서비스에서 처리하지 않는다.
     */
    @Override
    public void updateWeeklyRanking() {
        throw new UnsupportedOperationException("주간 랭킹 업데이트는 WeeklyRankingService에서 처리합니다.");
    }
}