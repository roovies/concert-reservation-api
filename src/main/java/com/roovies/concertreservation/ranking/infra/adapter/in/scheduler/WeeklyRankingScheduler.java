package com.roovies.concertreservation.ranking.infra.adapter.in.scheduler;

import com.roovies.concertreservation.ranking.application.port.in.UpdateRankingUseCase;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 주간 랭킹 스케줄러.
 * <p>
 * 주기적으로 주간 랭킹을 업데이트한다.
 */
@Slf4j
@Component
public class WeeklyRankingScheduler {

    private final UpdateRankingUseCase updateRankingUseCase;

    public WeeklyRankingScheduler(@Qualifier("weeklyRankingService") UpdateRankingUseCase updateRankingUseCase) {
        this.updateRankingUseCase = updateRankingUseCase;
    }

    /**
     * 주간 랭킹 업데이트 스케줄러.
     * <p>
     * 매주 월요일 0시(자정)에 주간 랭킹을 재계산하여 Redis에 캐싱한다.
     * ShedLock을 사용해 분산 환경에서 중복 실행을 방지한다.
     */
    @Scheduled(cron = "0 0 0 * * MON")  // 매주 월요일 00:00
    @SchedulerLock(name = "updateWeeklyRanking", lockAtMostFor = "10m", lockAtLeastFor = "1m")
    public void updateWeeklyRanking() {
        log.info("=== 주간 랭킹 업데이트 시작 ===");
        try {
            updateRankingUseCase.updateWeeklyRanking();
            log.info("=== 주간 랭킹 업데이트 완료 ===");
        } catch (Exception e) {
            log.error("=== 주간 랭킹 업데이트 실패 ===", e);
        }
    }
}