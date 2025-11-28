package com.roovies.concertreservation.ranking.infra.adapter.in.web;

import com.roovies.concertreservation.ranking.application.dto.result.ConcertRankingResult;
import com.roovies.concertreservation.ranking.application.port.in.GetRealtimeRankingUseCase;
import com.roovies.concertreservation.ranking.application.port.in.GetWeeklyRankingUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 랭킹 조회 REST API 컨트롤러.
 * <p>
 * 실시간 및 주간 콘서트 예약 랭킹을 제공한다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rankings")
@RequiredArgsConstructor
@Tag(name = "랭킹 API", description = "콘서트 예약 랭킹 조회")
public class RankingController {

    @Qualifier("realtimeRankingService")
    private final GetRealtimeRankingUseCase getRealtimeRankingUseCase;

    @Qualifier("weeklyRankingService")
    private final GetWeeklyRankingUseCase getWeeklyRankingUseCase;

    /**
     * 실시간 랭킹 Top 5를 조회한다.
     * <p>
     * 최근 24시간 기준 결제 건수가 많은 콘서트 스케줄 순으로 반환한다.
     *
     * @return 실시간 랭킹 목록 (1-5위)
     */
    @GetMapping("/realtime")
    @Operation(summary = "실시간 랭킹 조회", description = "최근 24시간 기준 결제 건수 기반 Top 5 랭킹을 조회합니다.")
    public ResponseEntity<List<ConcertRankingResult>> getRealtimeRanking() {
        log.info("[RankingController] 실시간 랭킹 조회 요청");

        List<ConcertRankingResult> rankings = getRealtimeRankingUseCase.getRealtimeRanking();

        log.info("[RankingController] 실시간 랭킹 조회 완료 - {} 건", rankings.size());
        return ResponseEntity.ok(rankings);
    }

    /**
     * 주간 랭킹 Top 5를 조회한다.
     * <p>
     * 최근 일주일 기준 결제 건수가 많은 콘서트 스케줄 순으로 반환한다.
     *
     * @return 주간 랭킹 목록 (1-5위)
     */
    @GetMapping("/weekly")
    @Operation(summary = "주간 랭킹 조회", description = "최근 일주일 기준 결제 건수 기반 Top 5 랭킹을 조회합니다.")
    public ResponseEntity<List<ConcertRankingResult>> getWeeklyRanking() {
        log.info("[RankingController] 주간 랭킹 조회 요청");

        List<ConcertRankingResult> rankings = getWeeklyRankingUseCase.getWeeklyRanking();

        log.info("[RankingController] 주간 랭킹 조회 완료 - {} 건", rankings.size());
        return ResponseEntity.ok(rankings);
    }
}