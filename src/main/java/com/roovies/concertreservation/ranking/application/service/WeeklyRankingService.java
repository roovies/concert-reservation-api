package com.roovies.concertreservation.ranking.application.service;

import com.roovies.concertreservation.ranking.application.dto.query.GetWeeklyPaymentCountQuery;
import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;
import com.roovies.concertreservation.ranking.application.port.in.GetWeeklyRankingUseCase;
import com.roovies.concertreservation.ranking.application.port.in.UpdateRankingUseCase;
import com.roovies.concertreservation.ranking.application.port.out.RankingCachePort;
import com.roovies.concertreservation.ranking.application.port.out.RankingQueryRepositoryPort;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 주간 랭킹 서비스.
 * <p>
 * 일주일 기준 결제 건수를 기반으로 주간 랭킹을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Qualifier("weeklyRankingService")
public class WeeklyRankingService implements GetWeeklyRankingUseCase, UpdateRankingUseCase {

    @Qualifier("rankingRedisCache")
    private final RankingCachePort rankingCachePort;

    @Qualifier("rankingQueryRepository")
    private final RankingQueryRepositoryPort rankingQueryRepositoryPort;

    /**
     * 주간 랭킹 Top 5를 조회한다.
     *
     * @return 주간 랭킹 목록 (1-5위)
     */
    @Override
    public List<ConcertRankingResult> getWeeklyRanking() {
        log.info("[WeeklyRankingService] 주간 랭킹 조회");

        List<ConcertRanking> rankings = rankingCachePort.getWeeklyTop5();

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
     * 주간 랭킹을 재계산하여 업데이트한다.
     * <p>
     * DB에서 최근 일주일간의 결제 건수를 조회하여 Redis에 캐싱한다.
     */
    @Override
    public void updateWeeklyRanking() {
        log.info("[WeeklyRankingService] 주간 랭킹 업데이트 시작");

        // 일주일 전 ~ 현재
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(7);

        GetWeeklyPaymentCountQuery query = GetWeeklyPaymentCountQuery.of(startDate, endDate);

        // DB에서 주간 결제 건수 Top 5 조회
        List<ConcertRanking> weeklyTop5 = rankingQueryRepositoryPort.findWeeklyTop5(query);

        // Redis에 캐싱
        rankingCachePort.saveWeeklyRanking(weeklyTop5);

        log.info("[WeeklyRankingService] 주간 랭킹 업데이트 완료 - {} 건", weeklyTop5.size());
    }

    /**
     * 실시간 랭킹 업데이트는 이 서비스에서 처리하지 않는다.
     */
    @Override
    public void updateRealtimeRanking(Long scheduleId) {
        throw new UnsupportedOperationException("실시간 랭킹 업데이트는 RealtimeRankingService에서 처리합니다.");
    }
}