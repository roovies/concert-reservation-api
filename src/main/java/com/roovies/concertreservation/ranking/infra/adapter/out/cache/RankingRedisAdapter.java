package com.roovies.concertreservation.ranking.infra.adapter.out.cache;

import com.roovies.concertreservation.ranking.application.port.out.RankingCachePort;
import com.roovies.concertreservation.ranking.application.port.out.RankingQueryRepositoryPort;
import com.roovies.concertreservation.ranking.domain.entity.ConcertRanking;
import com.roovies.concertreservation.ranking.domain.vo.RankingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 랭킹 Redis 어댑터.
 * <p>
 * Redis Sorted Set을 사용하여 실시간/주간 랭킹을 저장하고 조회한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Qualifier("rankingRedisCache")
public class RankingRedisAdapter implements RankingCachePort {

    private static final String REALTIME_RANKING_KEY = "ranking:realtime";
    private static final String WEEKLY_RANKING_KEY = "ranking:weekly";
    private static final int TOP_N = 5;
    private static final long REALTIME_TTL_HOURS = 24;

    private final RedissonClient redisson;

    @Qualifier("rankingQueryRepository")
    private final RankingQueryRepositoryPort rankingQueryRepositoryPort;

    /**
     * 실시간 랭킹에 스케줄의 결제 건수를 증가시킨다.
     * <p>
     * Redis Sorted Set의 스코어를 1씩 증가시킨다.
     * 24시간 TTL을 설정하여 자동으로 오래된 데이터를 제거한다.
     *
     * @param scheduleId 스케줄 ID
     */
    @Override
    public void incrementRealtimeRanking(Long scheduleId) {
        RScoredSortedSet<String> sortedSet = redisson.getScoredSortedSet(REALTIME_RANKING_KEY);

        String key = String.valueOf(scheduleId);

        // 스코어 증가 (없으면 0에서 시작)
        sortedSet.addScore(key, 1.0);

        // TTL 설정 (24시간)
        sortedSet.expire(REALTIME_TTL_HOURS, TimeUnit.HOURS);

        log.debug("[RankingRedisAdapter] 실시간 랭킹 스코어 증가 - scheduleId: {}", scheduleId);
    }

    /**
     * 실시간 랭킹 Top 5를 조회한다.
     * <p>
     * Redis Sorted Set에서 스코어가 높은 순으로 5개를 조회한다.
     *
     * @return 실시간 랭킹 목록 (1-5위)
     */
    @Override
    public List<ConcertRanking> getRealtimeTop5() {
        RScoredSortedSet<String> sortedSet = redisson.getScoredSortedSet(REALTIME_RANKING_KEY);

        // 스코어가 높은 순으로 Top 5 조회 (역순)
        Collection<String> topScheduleIds = sortedSet.valueRangeReversed(0, TOP_N - 1);

        List<ConcertRanking> rankings = new ArrayList<>();
        int rank = 1;

        for (String scheduleIdStr : topScheduleIds) {
            Long scheduleId = Long.parseLong(scheduleIdStr);
            Double score = sortedSet.getScore(scheduleIdStr);

            // 스케줄ID로 콘서트 정보 조회
            RankingQueryRepositoryPort.ScheduleConcertInfo concertInfo =
                    rankingQueryRepositoryPort.findConcertInfoByScheduleId(scheduleId);

            ConcertRanking ranking = ConcertRanking.createRealtime(
                    concertInfo.scheduleId(),
                    concertInfo.concertId(),
                    concertInfo.concertTitle(),
                    score != null ? score.longValue() : 0L,
                    rank++
            );

            rankings.add(ranking);
        }

        return rankings;
    }

    /**
     * 주간 랭킹을 저장한다.
     * <p>
     * 기존 주간 랭킹을 삭제하고 새로운 랭킹을 저장한다.
     *
     * @param rankings 주간 랭킹 목록
     */
    @Override
    public void saveWeeklyRanking(List<ConcertRanking> rankings) {
        RScoredSortedSet<String> sortedSet = redisson.getScoredSortedSet(WEEKLY_RANKING_KEY);

        // 기존 데이터 삭제
        sortedSet.clear();

        // 새로운 랭킹 저장
        for (ConcertRanking ranking : rankings) {
            String key = String.valueOf(ranking.getScheduleId());
            sortedSet.add(ranking.getPaymentCount().doubleValue(), key);
        }

        log.info("[RankingRedisAdapter] 주간 랭킹 저장 완료 - {} 건", rankings.size());
    }

    /**
     * 주간 랭킹 Top 5를 조회한다.
     *
     * @return 주간 랭킹 목록 (1-5위)
     */
    @Override
    public List<ConcertRanking> getWeeklyTop5() {
        RScoredSortedSet<String> sortedSet = redisson.getScoredSortedSet(WEEKLY_RANKING_KEY);

        // 스코어가 높은 순으로 Top 5 조회 (역순)
        Collection<String> topScheduleIds = sortedSet.valueRangeReversed(0, TOP_N - 1);

        List<ConcertRanking> rankings = new ArrayList<>();
        int rank = 1;

        for (String scheduleIdStr : topScheduleIds) {
            Long scheduleId = Long.parseLong(scheduleIdStr);
            Double score = sortedSet.getScore(scheduleIdStr);

            // 스케줄ID로 콘서트 정보 조회
            RankingQueryRepositoryPort.ScheduleConcertInfo concertInfo =
                    rankingQueryRepositoryPort.findConcertInfoByScheduleId(scheduleId);

            ConcertRanking ranking = ConcertRanking.createWeekly(
                    concertInfo.scheduleId(),
                    concertInfo.concertId(),
                    concertInfo.concertTitle(),
                    score != null ? score.longValue() : 0L,
                    rank++
            );

            rankings.add(ranking);
        }

        return rankings;
    }

    /**
     * 주간 랭킹 캐시를 초기화한다.
     */
    @Override
    public void clearWeeklyRanking() {
        RScoredSortedSet<String> sortedSet = redisson.getScoredSortedSet(WEEKLY_RANKING_KEY);
        sortedSet.clear();
        log.info("[RankingRedisAdapter] 주간 랭킹 초기화 완료");
    }
}